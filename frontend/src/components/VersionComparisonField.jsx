import { useId } from 'react';
import { Select } from './ui';

export default function VersionComparisonField({ value, onChange }) {
  const id = useId();

  return (
    <div>
      <label htmlFor={id} className="label-sm mb-xs display-block">Version comparison</label>
      <Select id={id} value={value} onChange={e => onChange(e.target.value)} aria-describedby={`${id}-help`}>
        <option value="ANY_DIFFERENT">Any different version</option>
        <option value="HIGHER_ONLY">Higher version only</option>
      </Select>
      <p id={`${id}-help`} className="body-sm text-mute mt-xs">
        {value === 'HIGHER_ONLY'
          ? 'Higher numbers only, ignoring v/V. Non-numeric tags use exact comparison.'
          : 'Any tag change counts, including lower versions.'}
      </p>
    </div>
  );
}
