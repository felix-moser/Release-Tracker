import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { Button, Card } from '../components/ui';
import RepoModal from '../components/RepoModal';
import DockerImagesSection from '../components/DockerImagesSection';
import { shortDigest } from '../formatters';

export default function RepositoriesView() {
  const [repos, setRepos] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const load = async () => {
    try {
      const [reposData, templatesData] = await Promise.all([api.getRepos(), api.getTemplates()]);
      setRepos(reposData); setTemplates(templatesData); setError(''); return reposData;
    } catch (e) { setError(e.message); return []; }
    finally { setLoading(false); }
  };
  useEffect(() => {
    load().then(repos => {
      const name = new URLSearchParams(window.location.search).get('edit');
      const repo = repos.find(r => r.repo === name);
      if (repo) handleEditClick(repo);
    });
  }, []);
  function handleEditClick(repo) { setEditing(repo); setIsModalOpen(true); }
  const handleCreateClick = () => { setEditing(null); setIsModalOpen(true); };
  const handleDelete = async name => {
    try { await api.deleteRepo(name); await load(); } catch (e) { setError(e.message); }
  };
  const save = async (payload, original) => {
    try {
      if (original) await api.updateRepo(original, payload); else await api.createRepo(payload);
      setIsModalOpen(false); await load();
    } catch (e) { alert(e.message); }
  };
  return <div className="container mt-xl">
    <div className="flex justify-between items-center mb-lg flex-col-mobile gap-sm">
      <div><h1 className="heading-lg">Tracked Repositories</h1><p className="body-md text-mute">GitHub repositories monitored for new releases.</p></div>
      <Button variant="primary-sm" onClick={handleCreateClick}>Add Repository</Button>
    </div>
    {error && <p role="alert" className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>{error}</p>}
    {loading ? <p className="body-md text-mute">Loading...</p> : repos.length === 0 ? <Card className="text-center py-xl"><p className="body-md text-mute">No repositories tracked yet.</p></Card> : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th style={{ textAlign: 'center' }}>Name</th>
                <th style={{ textAlign: 'center' }}>GitHub URL</th>
                <th style={{ textAlign: 'center' }}>Latest Tag</th>
                <th style={{ textAlign: 'center' }}>Template</th>
                <th>Linked Docker image</th>
                <th style={{ textAlign: 'center' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {repos.map(r => (
                <tr key={r.repo}>
                  <td style={{ textAlign: 'center', verticalAlign: 'middle' }}><strong>{r.repo}</strong></td>
                  <td className="body-sm" style={{ textAlign: 'center', verticalAlign: 'middle', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: '200px' }} title={r.repo_url}>
                    {r.repo_url.replace('https://api.github.com/repos/', '').replace('/releases/latest', '')}
                  </td>
                  <td style={{ textAlign: 'center', verticalAlign: 'middle' }}><span className="mono-eyebrow px-xs border rounded-sm">{r.latest_tag || 'N/A'}</span></td>
                  <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>{r.template_name || '-'}</td>
                  <td>{r.docker_image ? <><strong>{r.docker_image}</strong> <span className="mono-eyebrow token-badge">:latest</span><br /><span className="mono-eyebrow" title={r.docker_digest}>{shortDigest(r.docker_digest) || '-'}</span></> : '-'}</td>
                  <td style={{ textAlign: 'center', verticalAlign: 'middle' }}>
                    <div className="flex justify-center gap-sm">
                      <Button variant="ghost-sm" onClick={() => handleEditClick(r)}>Edit</Button>
                      <Button variant="danger" onClick={() => handleDelete(r.repo)}>Remove</Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
    )}
    <DockerImagesSection templates={templates} />
    <RepoModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onSave={save} templates={templates} initialData={editing} />
  </div>;
}
