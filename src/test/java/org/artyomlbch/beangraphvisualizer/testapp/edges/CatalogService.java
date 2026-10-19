package org.artyomlbch.beangraphvisualizer.testapp.edges;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

// CatalogService создается после CacheWarmer
@Service
@DependsOn("cacheWarmer")
public class CatalogService {
}

@Component
class CacheWarmer {
}