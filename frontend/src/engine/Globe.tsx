import React from 'react';
import DeckGL from '@deck.gl/react';
import { IconLayer } from '@deck.gl/layers';
import type { Entity } from '../network/useTelemetry';

interface GlobeProps {
    entities: Entity[];
    onViewportChange?: (viewState: any) => void;
}

const INITIAL_VIEW_STATE = {
    longitude: -74,
    latitude: 40.7,
    zoom: 3,
    pitch: 45,
    bearing: 0
};

export const Globe: React.FC<GlobeProps> = ({ entities, onViewportChange }) => {
    const layers = [
        new IconLayer({
            id: 'entity-layer',
            data: entities,
            pickable: true,
            iconAtlas: 'https://raw.githubusercontent.com/visgl/deck.gl-data/master/website/icon-atlas.png',
            iconMapping: {
                marker: {x: 0, y: 0, width: 128, height: 128, mask: true}
            },
            getIcon: () => 'marker',
            getPosition: (d: Entity) => [d.longitude, d.latitude, d.altitude],
            getSize: (d: Entity) => (d.type === 'SATELLITE' ? 20 : 15),
            getColor: (d: Entity) => {
                if (d.type === 'FLIGHT') return [0, 150, 255];
                if (d.type === 'SHIP') return [0, 255, 150];
                return [255, 150, 0];
            },
            getAngle: (d: Entity) => -d.heading
        })
    ];

    return (
        <DeckGL
            initialViewState={INITIAL_VIEW_STATE}
            controller={true}
            layers={layers}
            onViewStateChange={({ viewState }) => {
                if (onViewportChange) onViewportChange(viewState);
            }}
        />
    );
};
