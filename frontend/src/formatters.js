export const shortDigest = digest => (digest || '').replace(/^sha256:/, '').slice(0, 12);
