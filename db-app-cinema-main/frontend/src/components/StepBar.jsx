const STEPS = ['상영시간', '좌석', '결제', '완료'];

export default function StepBar({ current }) {
  return (
    <ol className="steps" aria-label="예매 단계">
      {STEPS.map((label, i) => (
        <li
          key={label}
          className={i < current ? 'done' : i === current ? 'current' : ''}
          aria-current={i === current ? 'step' : undefined}
        >
          {i + 1}. {label}
        </li>
      ))}
    </ol>
  );
}
