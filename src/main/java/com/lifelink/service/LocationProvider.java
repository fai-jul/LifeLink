package com.lifelink.service;

import java.util.Optional;

/** Resolves a project location into coordinates through a data provider. */
public interface LocationProvider {

    Optional<LocationService.Coordinates> geocode(String location);
}
