import { useEffect, useState, useRef } from 'react';
import { com } from '../proto/generated';

const GeoEntity = com.aetheris.shared.proto.GeoEntity;

export interface Entity {
    id: string;
    type: string;
    latitude: number;
    longitude: number;
    altitude: number;
    velocity: number;
    heading: number;
    timestamp: number;
}

export function useTelemetry() {
    const [entities, setEntities] = useState<Record<string, Entity>>({});
    const socketRef = useRef<WebSocket | null>(null);

    useEffect(() => {
        const socket = new WebSocket('ws://localhost:8081/ws/telemetry');
        socket.binaryType = 'arraybuffer';
        socketRef.current = socket;

        socket.onmessage = (event) => {
            try {
                const buffer = new Uint8Array(event.data);
                const message = GeoEntity.decode(buffer);
                
                setEntities(prev => ({
                    ...prev,
                    [message.id]: {
                        id: message.id,
                        type: GeoEntity.EntityType[message.type],
                        latitude: message.latitude,
                        longitude: message.longitude,
                        altitude: message.altitude,
                        velocity: message.velocity,
                        heading: message.heading,
                        timestamp: Number(message.timestamp),
                    }
                }));
            } catch (err) {
                console.error("Protobuf decode error", err);
            }
        };

        return () => socket.close();
    }, []);

    const updateViewport = (bounds: { minLat: number; maxLat: number; minLon: number; maxLon: number }) => {
        if (socketRef.current?.readyState === WebSocket.OPEN) {
            socketRef.current.send(JSON.stringify(bounds));
        }
    };

    return { entities: Object.values(entities), updateViewport };
}
