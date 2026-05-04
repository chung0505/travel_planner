import { useState, useCallback, useRef } from 'react'
import { GoogleMap, Marker, Polyline, InfoWindow, useJsApiLoader } from '@react-google-maps/api'
import type { AttractionResponse, TransportationMethod } from '../../types'

interface MapViewProps {
  geometry: [number, number][]
  attractions: AttractionResponse[]
  transportationMethod?: TransportationMethod
}

const POLYLINE_STYLE: Record<TransportationMethod, google.maps.PolylineOptions> = {
  WALKING:        { strokeColor: '#22c55e', strokeWeight: 3, strokeOpacity: 0,
                    icons: [{ icon: { path: 'M 0,-1 0,1', strokeOpacity: 1, scale: 3 }, offset: '0', repeat: '12px' }] },
  PUBLIC_TRANSIT: { strokeColor: '#3b82f6', strokeWeight: 5, strokeOpacity: 0.9 },
  TAXI:           { strokeColor: '#f97316', strokeWeight: 4, strokeOpacity: 0.85 },
}

const containerStyle = {
  height: '400px',
  width: '100%',
  borderRadius: '0.5rem',
}

export default function MapView({ geometry, attractions, transportationMethod }: MapViewProps) {
  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: import.meta.env.VITE_GOOGLE_MAPS_API_KEY ?? '',
  })

  const mapRef = useRef<google.maps.Map | null>(null)
  const [selectedIdx, setSelectedIdx] = useState<number | null>(null)

  const center = geometry.length > 0
    ? { lat: geometry[Math.floor(geometry.length / 2)][0], lng: geometry[Math.floor(geometry.length / 2)][1] }
    : { lat: 25.0330, lng: 121.5654 }

  const path = geometry.map(([lat, lng]) => ({ lat, lng }))
  const markers = attractions.filter(a => a.latitude != null && a.longitude != null)

  const onLoad = useCallback((map: google.maps.Map) => {
    mapRef.current = map
    if (geometry.length > 0) {
      const bounds = new google.maps.LatLngBounds()
      geometry.forEach(([lat, lng]) => bounds.extend({ lat, lng }))
      map.fitBounds(bounds, 40)
    }
  }, [geometry])

  if (!isLoaded) {
    return (
      <div style={containerStyle} className="bg-gray-100 flex items-center justify-center rounded-lg">
        <span className="text-gray-500 text-sm">地圖載入中...</span>
      </div>
    )
  }

  return (
    <GoogleMap
      mapContainerStyle={containerStyle}
      center={center}
      zoom={13}
      onLoad={onLoad}
    >
      {path.length > 1 && (
        <Polyline
          path={path}
          options={transportationMethod ? POLYLINE_STYLE[transportationMethod] : { strokeColor: '#3b82f6', strokeWeight: 4, strokeOpacity: 0.8 }}
        />
      )}

      {markers.map((a, idx) => (
        <Marker
          key={a.id}
          position={{ lat: a.latitude!, lng: a.longitude! }}
          label={{ text: String(idx + 1), color: 'white', fontWeight: 'bold' }}
          onClick={() => setSelectedIdx(idx)}
        >
          {selectedIdx === idx && (
            <InfoWindow onCloseClick={() => setSelectedIdx(null)}>
              <div>
                <div className="text-sm font-medium">{idx + 1}. {a.name}</div>
                <div className="text-xs text-gray-500">{a.startTime} – {a.endTime}</div>
              </div>
            </InfoWindow>
          )}
        </Marker>
      ))}
    </GoogleMap>
  )
}
