import * as $protobuf from "protobufjs";
import Long = require("long");
/** Namespace com. */
export namespace com {

    /** Namespace aetheris. */
    namespace aetheris {

        /** Namespace shared. */
        namespace shared {

            /** Namespace proto. */
            namespace proto {

                /** Properties of a GeoEntity. */
                interface IGeoEntity {

                    /** GeoEntity id */
                    id?: (string|null);

                    /** GeoEntity type */
                    type?: (com.aetheris.shared.proto.GeoEntity.EntityType|null);

                    /** GeoEntity latitude */
                    latitude?: (number|null);

                    /** GeoEntity longitude */
                    longitude?: (number|null);

                    /** GeoEntity altitude */
                    altitude?: (number|null);

                    /** GeoEntity velocity */
                    velocity?: (number|null);

                    /** GeoEntity heading */
                    heading?: (number|null);

                    /** GeoEntity timestamp */
                    timestamp?: (number|Long|null);

                    /** GeoEntity compressedMetadata */
                    compressedMetadata?: (Uint8Array|null);
                }

                /** Represents a GeoEntity. */
                class GeoEntity implements IGeoEntity {

                    /**
                     * Constructs a new GeoEntity.
                     * @param [properties] Properties to set
                     */
                    constructor(properties?: com.aetheris.shared.proto.IGeoEntity);

                    /** GeoEntity id. */
                    public id: string;

                    /** GeoEntity type. */
                    public type: com.aetheris.shared.proto.GeoEntity.EntityType;

                    /** GeoEntity latitude. */
                    public latitude: number;

                    /** GeoEntity longitude. */
                    public longitude: number;

                    /** GeoEntity altitude. */
                    public altitude: number;

                    /** GeoEntity velocity. */
                    public velocity: number;

                    /** GeoEntity heading. */
                    public heading: number;

                    /** GeoEntity timestamp. */
                    public timestamp: (number|Long);

                    /** GeoEntity compressedMetadata. */
                    public compressedMetadata: Uint8Array;

                    /**
                     * Creates a new GeoEntity instance using the specified properties.
                     * @param [properties] Properties to set
                     * @returns GeoEntity instance
                     */
                    public static create(properties?: com.aetheris.shared.proto.IGeoEntity): com.aetheris.shared.proto.GeoEntity;

                    /**
                     * Encodes the specified GeoEntity message. Does not implicitly {@link com.aetheris.shared.proto.GeoEntity.verify|verify} messages.
                     * @param message GeoEntity message or plain object to encode
                     * @param [writer] Writer to encode to
                     * @returns Writer
                     */
                    public static encode(message: com.aetheris.shared.proto.IGeoEntity, writer?: $protobuf.Writer): $protobuf.Writer;

                    /**
                     * Encodes the specified GeoEntity message, length delimited. Does not implicitly {@link com.aetheris.shared.proto.GeoEntity.verify|verify} messages.
                     * @param message GeoEntity message or plain object to encode
                     * @param [writer] Writer to encode to
                     * @returns Writer
                     */
                    public static encodeDelimited(message: com.aetheris.shared.proto.IGeoEntity, writer?: $protobuf.Writer): $protobuf.Writer;

                    /**
                     * Decodes a GeoEntity message from the specified reader or buffer.
                     * @param reader Reader or buffer to decode from
                     * @param [length] Message length if known beforehand
                     * @returns GeoEntity
                     * @throws {Error} If the payload is not a reader or valid buffer
                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                     */
                    public static decode(reader: ($protobuf.Reader|Uint8Array), length?: number): com.aetheris.shared.proto.GeoEntity;

                    /**
                     * Decodes a GeoEntity message from the specified reader or buffer, length delimited.
                     * @param reader Reader or buffer to decode from
                     * @returns GeoEntity
                     * @throws {Error} If the payload is not a reader or valid buffer
                     * @throws {$protobuf.util.ProtocolError} If required fields are missing
                     */
                    public static decodeDelimited(reader: ($protobuf.Reader|Uint8Array)): com.aetheris.shared.proto.GeoEntity;

                    /**
                     * Verifies a GeoEntity message.
                     * @param message Plain object to verify
                     * @returns `null` if valid, otherwise the reason why it is not
                     */
                    public static verify(message: { [k: string]: any }): (string|null);

                    /**
                     * Creates a GeoEntity message from a plain object. Also converts values to their respective internal types.
                     * @param object Plain object
                     * @returns GeoEntity
                     */
                    public static fromObject(object: { [k: string]: any }): com.aetheris.shared.proto.GeoEntity;

                    /**
                     * Creates a plain object from a GeoEntity message. Also converts values to other types if specified.
                     * @param message GeoEntity
                     * @param [options] Conversion options
                     * @returns Plain object
                     */
                    public static toObject(message: com.aetheris.shared.proto.GeoEntity, options?: $protobuf.IConversionOptions): { [k: string]: any };

                    /**
                     * Converts this GeoEntity to JSON.
                     * @returns JSON object
                     */
                    public toJSON(): { [k: string]: any };

                    /**
                     * Gets the default type url for GeoEntity
                     * @param [typeUrlPrefix] your custom typeUrlPrefix(default "type.googleapis.com")
                     * @returns The default type url
                     */
                    public static getTypeUrl(typeUrlPrefix?: string): string;
                }

                namespace GeoEntity {

                    /** EntityType enum. */
                    enum EntityType {
                        UNKNOWN = 0,
                        FLIGHT = 1,
                        SHIP = 2,
                        SATELLITE = 3
                    }
                }
            }
        }
    }
}
