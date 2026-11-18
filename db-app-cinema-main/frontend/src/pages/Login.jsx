import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { isMock } from '../api/client.js';

const TEST_ACCOUNTS = [
  { login_id: 'adult01', password: 'test1234', note: '1999.03.14생, 성인' },
  { login_id: 'teen01', password: 'test1234', note: '2012.05.20생, 만 14세 (15·19세 영화 예매 불가)' },
];

export default function Login() {
  const { member, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ login_id: '', password: '' });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const from = location.state?.from ?? '/movies';
  if (member) return <Navigate to={from} replace />;

  async function onSubmit(e) {
    e.preventDefault();
    setError('');
    setBusy(true);
    try {
      await login(form.login_id.trim(), form.password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login">
      <form className="login-form" onSubmit={onSubmit}>
        <h1 className="login-title">로그인</h1>
        <label className="field">
          <span>아이디</span>
          <input
            autoComplete="username"
            value={form.login_id}
            onChange={(e) => setForm({ ...form, login_id: e.target.value })}
            required
          />
        </label>
        <label className="field">
          <span>비밀번호</span>
          <input
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
        </label>
        {error && (
          <p className="form-error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={busy}>
          {busy ? '로그인하는 중…' : '로그인'}
        </button>
      </form>

      {isMock && (
        <div className="test-accounts">
          <p>테스트 계정 (비밀번호 test1234). 아이디를 누르면 입력됩니다.</p>
          <ul>
            {TEST_ACCOUNTS.map((a) => (
              <li key={a.login_id}>
                <button
                  type="button"
                  onClick={() => setForm({ login_id: a.login_id, password: a.password })}
                >
                  {a.login_id}
                </button>
                <span>{a.note}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
