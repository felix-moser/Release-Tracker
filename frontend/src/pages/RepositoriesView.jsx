import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { Button, Input, Select, Card, Modal } from '../components/ui';

export default function RepositoriesView() {
  const [repos, setRepos] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRepoName, setEditingRepoName] = useState(null);

  // Form state
  const [name, setName] = useState('');
  const [githubUrl, setGithubUrl] = useState('');
  const [template, setTemplate] = useState('');

  const fetchReposAndTemplates = async () => {
    setLoading(true);
    try {
      const [reposData, templatesData] = await Promise.all([
        api.getRepos(),
        api.getTemplates()
      ]);
      setRepos(reposData);
      setTemplates(templatesData);
      if (templatesData.length > 0) {
        setTemplate(templatesData[0].name);
      }
      setError(null);
      return { reposData, templatesData };
    } catch (err) {
      setError(err.message);
      return { reposData: [], templatesData: [] };
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReposAndTemplates().then(({ reposData }) => {
      const editName = new URLSearchParams(window.location.search).get('edit');
      if (editName && reposData && reposData.length > 0) {
        const repo = reposData.find(r => r.repo === editName);
        if (repo) {
          handleEditClick(repo);
        }
      }
    });
  }, []);

  const handleDelete = async (repoName) => {
    try {
      await api.deleteRepo(repoName);
      fetchReposAndTemplates();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleCreateOrUpdate = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        name,
        github_url: githubUrl,
        template: template || undefined
      };
      
      if (editingRepoName) {
        await api.updateRepo(editingRepoName, payload);
      } else {
        await api.createRepo(payload);
      }
      setIsModalOpen(false);
      setEditingRepoName(null);
      setName('');
      setGithubUrl('');
      fetchReposAndTemplates();
    } catch (err) {
      alert(err.message);
    }
  };

  function handleEditClick(repo) {
    setEditingRepoName(repo.repo);
    setName(repo.repo);
    setGithubUrl(repo.repo_url);
    setTemplate(repo.template_name || '');
    setIsModalOpen(true);
  }

  function handleCreateClick() {
    setEditingRepoName(null);
    setName('');
    setGithubUrl('');
    setTemplate(templates.length > 0 ? templates[0].name : '');
    setIsModalOpen(true);
  }

  return (
    <div className="container mt-xl">
      <div className="flex justify-between items-center mb-lg flex-col-mobile gap-sm">
        <div>
          <h1 className="heading-lg">Tracked Repositories</h1>
          <p className="body-md text-mute">GitHub repositories monitored for new releases.</p>
        </div>
        <Button variant="primary" onClick={handleCreateClick}>Add Repository</Button>
      </div>

      {error && <div className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>Error: {error}</div>}

      {loading ? (
        <div className="body-md">Loading...</div>
      ) : repos.length === 0 ? (
        <Card className="text-center py-xl">
          <p className="body-lg mb-sm">No repositories tracked yet.</p>
          <Button variant="secondary" onClick={handleCreateClick}>Add your first repository</Button>
        </Card>
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th style={{ textAlign: 'center' }}>Name</th>
                <th style={{ textAlign: 'center' }}>GitHub URL</th>
                <th style={{ textAlign: 'center' }}>Latest Tag</th>
                <th style={{ textAlign: 'center' }}>Template</th>
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

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingRepoName ? "Edit Repository" : "Add Repository"}>
        <form onSubmit={handleCreateOrUpdate} className="flex-col gap-sm">
          <div>
            <label className="label-sm mb-xs display-block">Name (e.g., talos)</label>
            <Input value={name} onChange={e => setName(e.target.value)} required />
          </div>
          <div>
            <label className="label-sm mb-xs display-block">GitHub URL</label>
            <Input type="url" value={githubUrl} onChange={e => setGithubUrl(e.target.value)} placeholder="https://github.com/owner/repo" required />
          </div>
          <div>
            <label className="label-sm mb-xs display-block">Jira Template</label>
            {templates.length === 0 ? (
              <p className="body-sm text-mute border rounded-sm p-xs" style={{ padding: '8px' }}>
                No templates available. You can create one in the Templates view.
              </p>
            ) : (
              <Select value={template} onChange={e => setTemplate(e.target.value)}>
                <option value="">(None)</option>
                {templates.map(t => (
                  <option key={t.name} value={t.name}>{t.name}</option>
                ))}
              </Select>
            )}
          </div>
          <div className="flex justify-end gap-sm mt-md">
            <Button variant="ghost-sm" type="button" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button variant="primary" type="submit">{editingRepoName ? "Save Changes" : "Add"}</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
