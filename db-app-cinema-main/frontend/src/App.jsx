import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth.jsx';
import Header from './components/Header.jsx';
import Login from './pages/Login.jsx';
import MovieList from './pages/MovieList.jsx';
import MovieDetail from './pages/MovieDetail.jsx';
import ScheduleSelect from './pages/ScheduleSelect.jsx';
import SeatSelect from './pages/SeatSelect.jsx';
import Complete from './pages/Complete.jsx';
import MyPage from './pages/MyPage.jsx';

const guard = (el) => <RequireAuth>{el}</RequireAuth>;

export default function App() {
  return (
    <>
      <Header />
      <main className="page">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Navigate to="/movies" replace />} />
          <Route path="/movies" element={guard(<MovieList />)} />
          <Route path="/movies/:movieId" element={guard(<MovieDetail />)} />
          <Route path="/movies/:movieId/schedules" element={guard(<ScheduleSelect />)} />
          <Route path="/schedules/:scheduleId/seats" element={guard(<SeatSelect />)} />
          <Route path="/reservations/:reservationId/complete" element={guard(<Complete />)} />
          <Route path="/mypage" element={guard(<MyPage />)} />
          <Route path="*" element={<Navigate to="/movies" replace />} />
        </Routes>
      </main>
    </>
  );
}
