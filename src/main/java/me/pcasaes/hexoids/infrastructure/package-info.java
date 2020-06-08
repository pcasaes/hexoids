/**
 * Here we set up the low latency event loop using the LMAX Disruptor. We also find domain event producers and
 * wiring configuration. This layer leverages CDI to perform the IOC wiring.
 */
package me.pcasaes.hexoids.infrastructure;