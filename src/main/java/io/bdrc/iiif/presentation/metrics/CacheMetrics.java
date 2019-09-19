package io.bdrc.iiif.presentation.metrics;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Metrics;

@Service
public class CacheMetrics {

	public static void found() {
		Metrics.counter("cache.calls", "cache", "found").increment();
	}

	public static void notFound() {
		Metrics.counter("cache.calls", "cache", "noFound").increment();
	}

	public static void put() {
		Metrics.counter("cache.calls", "cache", "put").increment();
	}

}
