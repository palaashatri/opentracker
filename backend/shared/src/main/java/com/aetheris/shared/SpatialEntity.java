package com.aetheris.shared;

import com.aetheris.shared.proto.GeoEntity;

public interface SpatialEntity {
    String getId();
    GeoEntity.EntityType getType();
    double getLatitude();
    double getLongitude();
    float getAltitude();
    float getVelocity();
    float getHeading();
    long getTimestamp();
    byte[] getCompressedMetadata();

    default GeoEntity toProto() {
        return GeoEntity.newBuilder()
                .setId(getId())
                .setType(getType())
                .setLatitude(getLatitude())
                .setLongitude(getLongitude())
                .setAltitude(getAltitude())
                .setVelocity(getVelocity())
                .setHeading(getHeading())
                .setTimestamp(getTimestamp())
                .setCompressedMetadata(com.google.protobuf.ByteString.copyFrom(getCompressedMetadata() != null ? getCompressedMetadata() : new byte[0]))
                .build();
    }
}
