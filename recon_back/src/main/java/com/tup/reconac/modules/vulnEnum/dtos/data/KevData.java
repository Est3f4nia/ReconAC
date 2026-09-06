package com.tup.reconac.modules.vulnEnum.dtos.data;

import java.util.List;

// esquema publicado por CISA
public record KevData(
        String cveID,
        String vendorProject,
        String product,
        String vulnerabilityName,
        String dateAdded,
        String shortDescription,
        String requiredAction,
        String dueDate,
        String knownRansomwareCampaignUse,
        String notes,
        List<String> cwes
) {
}
