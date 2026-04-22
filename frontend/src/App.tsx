import { useState } from 'react';
import { Globe } from './engine/Globe';
import { useTelemetry } from './network/useTelemetry';
import { Search, Star, Settings, Info, Plane, Ship, Satellite } from 'lucide-react';

function App() {
  const { entities, updateViewport } = useTelemetry();
  const [selectedEntity] = useState<string | null>(null);

  const handleViewportChange = (viewState: any) => {
    // Convert viewState to bounding box and update backend
    // For this prototype, we'll send a rough estimation
    updateViewport({
      minLat: viewState.latitude - 10,
      maxLat: viewState.latitude + 10,
      minLon: viewState.longitude - 20,
      maxLon: viewState.longitude + 20
    });
  };

  return (
    <div className="h-screen w-screen overflow-hidden bg-black text-white font-sans">
      {/* 3D Engine */}
      <Globe entities={entities} onViewportChange={handleViewportChange} />

      {/* macOS Sidebar */}
      <div className="mac-sidebar mac-vibrancy">
        <h1 className="text-xl font-bold tracking-tight mb-4">Aetheris</h1>
        
        <div className="flex flex-col gap-2">
          <div className="flex items-center gap-3 p-2 rounded-lg bg-white/10">
            <Star size={18} /> <span>Favorites</span>
          </div>
          <div className="flex items-center gap-3 p-2 rounded-lg text-white/60 hover:bg-white/5 cursor-pointer">
            <Plane size={18} /> <span>Aviation</span>
          </div>
          <div className="flex items-center gap-3 p-2 rounded-lg text-white/60 hover:bg-white/5 cursor-pointer">
            <Ship size={18} /> <span>Maritime</span>
          </div>
          <div className="flex items-center gap-3 p-2 rounded-lg text-white/60 hover:bg-white/5 cursor-pointer">
            <Satellite size={18} /> <span>Orbital</span>
          </div>
        </div>

        <div className="mt-auto flex flex-col gap-2">
          <div className="flex items-center gap-3 p-2 text-white/40"><Settings size={18} /> <span>Settings</span></div>
          <div className="flex items-center gap-3 p-2 text-white/40"><Info size={18} /> <span>About</span></div>
        </div>
      </div>

      {/* Spotlight Search */}
      <div className="spotlight-search mac-vibrancy">
        <Search size={20} className="text-white/40 mr-3" />
        <input 
          type="text" 
          placeholder="Search flight, vessel, or satellite..." 
          className="bg-transparent border-none outline-none w-full text-white placeholder:text-white/20"
        />
        <div className="text-xs text-white/20 ml-2">⌘K</div>
      </div>

      {/* Inspector Panel */}
      {selectedEntity && (
        <div className="inspector-panel mac-panel mac-vibrancy">
          <h2 className="text-lg font-semibold mb-2">Entity Details</h2>
          <div className="text-sm text-white/60">
            ID: {selectedEntity}
          </div>
          {/* Detailed metadata from GraphQL would be fetched here */}
        </div>
      )}
    </div>
  );
}

export default App;
