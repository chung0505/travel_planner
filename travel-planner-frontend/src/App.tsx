import { BrowserRouter, Routes, Route } from 'react-router-dom'
import HomePage from './pages/HomePage'
import TripDetailPage from './pages/TripDetailPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/trips/:id" element={<TripDetailPage />} />
      </Routes>
    </BrowserRouter>
  )
}
