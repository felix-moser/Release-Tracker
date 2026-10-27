import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { Button, Card, Input, Modal, Select } from './ui';
import { shortDigest } from '../formatters';

export default function DockerImagesSection({ templates = [], layout = 'table', images: providedImages, onImagesChange }) {
  const [loadedImages, setImages] = useState([]);
  const images = providedImages ?? loadedImages;
  const [editing, setEditing] = useState(null);
  const [open, setOpen] = useState(false);
  const [name, setName] = useState('');
  const [template, setTemplate] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(providedImages === undefined);
  const load = async () => {
    try {
      const data = await api.getDockerImages();
      setImages(data);
      onImagesChange?.(data);
      setError('');
    }
    catch (e) { setError(e.message); }
    finally { setLoading(false); }
  };
  useEffect(() => {
    if (providedImages !== undefined) return;
    let cancelled = false;
    api.getDockerImages().then(data => {
      if (!cancelled) setImages(data);
    }).catch(e => {
      if (!cancelled) setError(e.message);
    }).finally(() => {
      if (!cancelled) setLoading(false);
    });
    return () => { cancelled = true; };
  }, [providedImages]);
  const edit = image => {
    setError(''); setEditing(image); setName(image?.image_name || ''); setTemplate(image?.template_name || ''); setOpen(true);
  };
  const save = async e => {
    e.preventDefault(); setSaving(true);
    try {
      const payload = { image_name: name, template_name: template || null };
      if (editing) await api.updateDockerImage(editing.image_name, payload);
      else await api.createDockerImage(payload);
      setOpen(false); await load();
    } catch (e) { setError(e.message); }
    finally { setSaving(false); }
  };
  const remove = async image => {
    try { await api.deleteDockerImage(image); await load(); }
    catch (e) { setError(e.message); }
  };
  return <section className="mt-xl mb-xl">
    <div className="flex justify-between items-center mb-lg flex-col-mobile gap-sm">
      <div><h2 className="heading-lg">Docker Images</h2><p className="body-sm text-mute">Standalone Docker images tracking :latest.</p></div>
      <Button variant="primary-sm" onClick={() => edit(null)}>Add Docker Image</Button>
    </div>
    {!open && error && <p role="alert" className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>{error}</p>}
    {loading ? <p className="body-md text-mute">Loading Docker images...</p> : layout === 'grid' ? (
      <div className="grid grid-cols-6 gap-lg">
        {images.map(image => <Card key={image.image_name} className="col-span-2 flex-col">
          <div className="flex justify-between items-center gap-sm mb-sm">
            <h3 className="heading-md">{image.image_name}</h3>
            <span className="mono-eyebrow token-badge">:latest</span>
          </div>
          <p className="mono-eyebrow mb-md" title={image.manifest_digest}>{shortDigest(image.manifest_digest) || 'No digest yet'}</p>
          <div className="mt-auto pt-md border-top">
            <span className="label-sm text-mute">Linked Template: </span>
            {image.template_name ? <Link className="body-sm" to={`/templates?edit=${encodeURIComponent(image.template_name)}`}>{image.template_name}</Link> : <span className="body-sm">None</span>}
            <div className="flex justify-end mt-sm"><Button variant="ghost-sm" onClick={() => edit(image)}>Edit</Button></div>
          </div>
        </Card>)}
        <button type="button" className="feature-card add-card col-span-1 flex-col text-center" onClick={() => edit(null)}>
          <span className="heading-lg text-mute">+</span>
          <span className="body-md text-mute">Add Docker Image</span>
        </button>
      </div>
    ) : images.length === 0 ? <Card className="text-center py-xl"><p className="body-md text-mute">No standalone Docker images tracked yet.</p></Card> : (
      <div className="table-container"><table><thead><tr><th>Image</th><th className="text-center">Short digest</th><th className="text-center">Template</th><th className="text-center">Actions</th></tr></thead>
        <tbody>{images.map(image => <tr key={image.image_name}>
          <td><strong>{image.image_name}</strong> <span className="mono-eyebrow token-badge">:latest</span></td>
          <td className="text-center" title={image.manifest_digest}><span className="mono-eyebrow token-badge">{shortDigest(image.manifest_digest) || '-'}</span></td>
          <td className="text-center">{image.template_name || '-'}</td>
          <td><div className="flex justify-center gap-sm"><Button variant="ghost-sm" onClick={() => edit(image)}>Edit</Button><Button variant="danger" onClick={() => remove(image.image_name)}>Remove</Button></div></td>
        </tr>)}</tbody></table></div>
    )}
    <Modal isOpen={open} onClose={() => setOpen(false)} title={editing ? 'Edit Docker Image' : 'Add Docker Image'}>
      <form onSubmit={save} className="flex-col gap-md">
        {error && <p role="alert" className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>{error}</p>}
        <div>
          <label htmlFor="standalone-image" className="label-sm mb-xs display-block">Docker Image</label>
          <Input id="standalone-image" value={name} onChange={e => setName(e.target.value)} placeholder="nginx or ghcr.io/owner/app" required />
          <p className="body-sm text-mute mt-xs">Always tracks :latest. Any supplied tag or digest is stripped.</p>
        </div>
        <div>
          <label htmlFor="standalone-template" className="label-sm mb-xs display-block">Docker Jira Template</label>
          <Select id="standalone-template" value={template} onChange={e => setTemplate(e.target.value)}><option value="">(None)</option>{templates.map(t => <option key={t.name} value={t.name}>{t.name}</option>)}</Select>
        </div>
        <div className="flex justify-end gap-sm mt-md"><Button type="button" variant="ghost-sm" onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" variant="primary-sm" disabled={saving}>{saving ? 'Saving...' : 'Save'}</Button></div>
      </form>
    </Modal>
  </section>;
}
