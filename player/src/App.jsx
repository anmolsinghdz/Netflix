import { useEffect, useRef, useState } from "react";
import Hls from "hls.js";
import { FaPlay } from "react-icons/fa";

export default function App() {
  const [movieId, setMovieId] = useState("");
  const [streamUrl, setStreamUrl] = useState("");
  const [error, setError] = useState("");

  const videoRef = useRef(null);

  useEffect(() => {
    if (!streamUrl || !videoRef.current) return;

    const video = videoRef.current;

    if (Hls.isSupported()) {
      const hls = new Hls();

      hls.loadSource(streamUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.ERROR, (_, data) => {
        console.error(data);
        setError("Failed to load stream");
      });

      return () => hls.destroy();
    }

    if (video.canPlayType("application/vnd.apple.mpegurl")) {
      video.src = streamUrl;
    }
  }, [streamUrl]);

  const handlePlay = () => {
    setError("");

    if (!movieId.trim()) return;

    setStreamUrl(
      `http://localhost:8084/api/v1/stream/${movieId}/playlist`
    );
  };

  return (
  <div className="min-h-screen bg-gradient-to-br from-black via-zinc-950 to-black text-white">
    {/* Background Glow */}
    <div className="fixed top-0 left-0 w-full h-full overflow-hidden -z-10">
      <div className="absolute top-20 left-1/4 w-96 h-96 bg-red-600/20 blur-[150px] rounded-full" />
      <div className="absolute bottom-20 right-1/4 w-96 h-96 bg-red-500/10 blur-[180px] rounded-full" />
    </div>

    {/* Header */}
    <div className="border-b border-zinc-800 backdrop-blur-xl bg-black/30">
      <div className="max-w-7xl mx-auto px-8 py-5 flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-black tracking-wider text-red-600">
            NETFLIX
          </h1>
          <p className="text-zinc-500 text-sm">
            Streaming Service Tester
          </p>
        </div>

        <div className="px-4 py-2 rounded-full bg-zinc-900 border border-zinc-800 text-sm">
          HLS Streaming
        </div>
      </div>
    </div>

    {/* Main Content */}
    <div className="max-w-7xl mx-auto px-8 py-10">
      {/* Control Panel */}
      <div className="bg-zinc-900/60 backdrop-blur-xl border border-zinc-800 rounded-2xl p-6 mb-8">
        <h2 className="text-xl font-semibold mb-4">
          Stream Endpoint Tester
        </h2>

        <div className="flex flex-col md:flex-row gap-4">
          <input
            type="text"
            placeholder="Enter Movie ID..."
            value={movieId}
            onChange={(e) => setMovieId(e.target.value)}
            className="
              flex-1
              px-5
              py-4
              rounded-xl
              bg-black/40
              border
              border-zinc-700
              focus:border-red-500
              focus:ring-2
              focus:ring-red-500/20
              outline-none
            "
          />

          <button
            onClick={handlePlay}
            className="
              px-8
              py-4
              rounded-xl
              bg-red-600
              hover:bg-red-700
              transition-all
              duration-200
              font-semibold
              flex
              items-center
              justify-center
              gap-3
              shadow-lg
              shadow-red-600/20
            "
          >
            <FaPlay />
            Play Stream
          </button>
        </div>

        {streamUrl && (
          <div className="mt-4 p-4 rounded-lg bg-black/50 border border-zinc-800">
            <p className="text-xs text-zinc-500 mb-1">
              Current Endpoint
            </p>
            <code className="text-green-400 break-all">
              {streamUrl}
            </code>
          </div>
        )}
      </div>

      {/* Video Player */}
      <div
        className="
          relative
          overflow-hidden
          rounded-3xl
          border
          border-zinc-800
          bg-black
          shadow-[0_0_50px_rgba(229,9,20,0.15)]
        "
      >
        <video
          ref={videoRef}
          controls
          autoPlay
          className="w-full aspect-video bg-black"
        />

        {!streamUrl && (
          <div className="absolute inset-0 flex flex-col items-center justify-center bg-black">
            <div className="w-24 h-24 rounded-full bg-red-600/10 flex items-center justify-center mb-6">
              <FaPlay
                size={30}
                className="text-red-500"
              />
            </div>

            <h3 className="text-2xl font-bold mb-2">
              No Stream Loaded
            </h3>

            <p className="text-zinc-500">
              Enter a Movie ID and start playback
            </p>
          </div>
        )}
      </div>

      {/* Stats Panel */}
      <div className="grid md:grid-cols-3 gap-6 mt-8">
        <div className="bg-zinc-900/60 border border-zinc-800 rounded-xl p-5">
          <p className="text-zinc-500 text-sm">Service</p>
          <h3 className="text-lg font-bold mt-2">
            Streaming Service
          </h3>
        </div>

        <div className="bg-zinc-900/60 border border-zinc-800 rounded-xl p-5">
          <p className="text-zinc-500 text-sm">Protocol</p>
          <h3 className="text-lg font-bold mt-2">
            HLS (.m3u8)
          </h3>
        </div>

        <div className="bg-zinc-900/60 border border-zinc-800 rounded-xl p-5">
          <p className="text-zinc-500 text-sm">Backend</p>
          <h3 className="text-lg font-bold mt-2">
            localhost:8084
          </h3>
        </div>
      </div>
    </div>
  </div>
);
}