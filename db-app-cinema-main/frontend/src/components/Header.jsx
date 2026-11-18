import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { isMock } from '../api/client.js';

export default function Header() {
  const { member, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <Link to="/movies" className="brand">
          별빛시네마
        </Link>
        {isMock && (
          <span className="mock-tag" title="백엔드 주소(VITE_API_BASE_URL)가 없어서 브라우저 안의 목업 데이터로 동작 중입니다.">
            목업 데이터
          </span>
        )}
        {member && (
          <nav className="topnav">
            <NavLink to="/movies">영화</NavLink>
            <NavLink to="/mypage">마이페이지</NavLink>
            <span className="topnav-user">{member.name}님</span>
            <button
              type="button"
              className="topnav-logout"
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              로그아웃
            </button>
          </nav>
        )}
      </div>
    </header>
  );
}
