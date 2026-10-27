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
          ? 'Compare dot-separated numbers, ignoring one leading v or V. For example, 1.10 is higher than 1.9, and 1.2 equals 1.2.0. If either tag cannot be compared numerically, any exact tag difference counts as an update.'
          : 'Any exact tag difference counts as an update, including lower versions and changes to the v or V prefix.'}
      </p>
    </div>
  );
}
