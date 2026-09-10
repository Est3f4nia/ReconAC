package com.tup.reconac.modules.vulnEnum;

import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.repositories.*;
import com.tup.reconac.modules.vulnEnum.services.*;
import com.tup.reconac.modules.vulnEnum.services.cache.VulnerabilityPersistenceService;
import com.tup.reconac.modules.vulnEnum.clients.EpssClient;
import com.tup.reconac.modules.vulnEnum.dtos.data.EpssData;
import com.tup.reconac.modules.vulnEnum.dtos.nvd.*;
import com.tup.reconac.cache.CacheService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@EnabledIfSystemProperty(named = "catalog.test.url", matches = ".+")
class CatalogConcurrencyPostgresTest {
    private LocalContainerEntityManagerFactoryBean factory;
    private TransactionTemplate tx;
    private JdbcTemplate jdbc;
    private VulnerabilityPersistenceService persistence;
    private EpssService epss;

    @BeforeEach void setup() {
        var ds = new DriverManagerDataSource(System.getProperty("catalog.test.url"), "postgres", "");
        jdbc = new JdbcTemplate(ds);
        factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(ds);
        factory.setPackagesToScan(Cpe.class.getPackageName());
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop",
                "hibernate.jdbc.time_zone", "UTC"));
        factory.afterPropertiesSet();
        var emf = factory.getObject();
        var manager = new JpaTransactionManager(emf);
        manager.setDataSource(ds);
        tx = new TransactionTemplate(manager);
        var repos = new JpaRepositoryFactory(SharedEntityManagerCreator.createSharedEntityManager(emf));
        var lock = new VulnerabilityCatalogLock(jdbc);
        var cves = repos.getRepository(CveRepository.class);
        persistence = new VulnerabilityPersistenceService(mock(RedisTemplate.class), JsonMapper.builder().build(),
                repos.getRepository(CpeRepository.class), cves, repos.getRepository(CweRepository.class),
                repos.getRepository(CpeCveRepository.class), repos.getRepository(CveCweRepository.class),
                repos.getRepository(ReferenciaRepository.class), lock);
        var client = mock(EpssClient.class);
        var data = mock(EpssData.class);
        when(data.epss()).thenReturn(new BigDecimal("0.1234"));
        when(client.getEpss(any())).thenReturn(Map.of("CVE-2007-4723", data));
        epss = new EpssService(client, mock(CacheService.class), cves, lock);
    }

    @AfterEach void close() { if (factory != null) factory.destroy(); }

    @Test void simultaneousNvdAndEpssKeepOneCveAndAllRelations() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        var ready = new CountDownLatch(4);
        var go = new CountDownLatch(1);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int writer = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    try { assertTrue(go.await(5, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new RuntimeException(e); }
                    tx.executeWithoutResult(status -> {
                        if (writer == 3) epss.enrichEpss(List.of("CVE-2007-4723"));
                        else persistence.persist(entry(writer == 2 ? "other" : "apache"));
                    });
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
            assertEquals(1, count("cve"));
            assertEquals(2, count("cpe"));
            assertEquals(2, count("cpe_cve"));
            assertEquals(1, count("cwe"));
            assertEquals(1, count("cve_cwe"));
            assertEquals(1, count("referencia"));
            assertEquals("HIGH", jdbc.queryForObject("select severidad from cve", String.class));
            assertEquals(new BigDecimal("0.1234"), jdbc.queryForObject("select epss from cve", BigDecimal.class));
        } finally { pool.shutdownNow(); }
    }

    @Test void rollbackReleasesLockAndDoesNotLeavePartialRows() throws Exception {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(status -> {
            persistence.persist(entry("apache"));
            throw new IllegalStateException("forced rollback");
        }));
        assertEquals(0, count("cve"));
        assertEquals(0, count("cpe"));
        var pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> tx.executeWithoutResult(status -> persistence.persist(entry("apache"))))
                    .get(10, TimeUnit.SECONDS);
            assertEquals(1, count("cve"));
            assertEquals(1, count("cpe_cve"));
        } finally { pool.shutdownNow(); }
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    private NvdCacheEntry entry(String vendor) {
        var now = LocalDateTime.now();
        var data = new NvdVulnerabilityData("CVE-2007-4723", "Concurrency regression", "HIGH",
                new BigDecimal("7.5"), "vector", now, now, List.of("CWE-79"),
                List.of(new NvdReferenceData("https://example.com/advisory", "vendor", List.of())),
                List.of(), "https://nvd.nist.gov/vuln/detail/CVE-2007-4723", null, null, null, null);
        return new NvdCacheEntry("cpe:/a:" + vendor + ":http_server:2.4.7", now, List.of(data));
    }
}
