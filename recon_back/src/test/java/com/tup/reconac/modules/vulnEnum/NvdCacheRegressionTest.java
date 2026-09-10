package com.tup.reconac.modules.vulnEnum;

import com.tup.reconac.cache.CacheService;
import com.tup.reconac.modules.vulnEnum.models.Cpe;
import com.tup.reconac.modules.vulnEnum.repositories.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NvdCacheRegressionTest {
    @Test void detectedCpeWithoutCompletedLookupIsCacheMiss() {
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class, RETURNS_DEEP_STUBS);
        var cpes = mock(CpeRepository.class);
        var links = mock(CpeCveRepository.class);
        var service = new CacheService(redis, cpes, mock(CveRepository.class),
                mock(CweRepository.class), links, mock(CveCweRepository.class), mock(ReferenciaRepository.class));
        var cpe = new Cpe();
        cpe.setUri("cpe:/a:apache:http_server:2.4.7");
        when(cpes.findByUri(cpe.getUri())).thenReturn(Optional.of(cpe));
        assertTrue(service.getNvd(cpe.getUri()).isEmpty());
        verifyNoInteractions(links);
    }
}
