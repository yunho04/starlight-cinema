import { useCallback, useEffect, useState } from 'react';

// 데이터를 불러오고 loading / error / reload를 함께 돌려주는 훅
export function useAsync(fn, deps) {
  const [state, setState] = useState({ data: null, error: null, loading: true });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(fn, deps);

  const load = useCallback(() => {
    let alive = true;
    setState((s) => ({ ...s, loading: true, error: null }));
    run()
      .then((data) => alive && setState({ data, error: null, loading: false }))
      .catch((error) => alive && setState({ data: null, error, loading: false }));
    return () => {
      alive = false;
    };
  }, [run]);

  useEffect(() => load(), [load]);
  return { ...state, reload: load };
}
