export function Loading({ text = '불러오는 중…' }) {
  return <p className="status">{text}</p>;
}

export function ErrorBox({ error, onRetry }) {
  return (
    <div className="notice notice-error" role="alert">
      <p>{error?.message ?? '알 수 없는 오류가 발생했습니다.'}</p>
      {onRetry && (
        <button type="button" className="btn btn-outline btn-sm" onClick={onRetry}>
          다시 불러오기
        </button>
      )}
    </div>
  );
}
