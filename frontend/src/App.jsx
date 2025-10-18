import { Routes, Route, Navigate, useLocation } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import Home from './pages/Home.jsx'
import Profile from './pages/Profile.jsx'

export default function App() {
  const location = useLocation()

  return (
    <>
      <Navbar />
      <div className="page">
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/profile" element={<Profile />} />

          {/* fallback */}
          <Route path="*" element={<Navigate to="/" replace state={{ from: location }} />} />
        </Routes>
      </div>
    </>
  )
}
