import React, { useState, useEffect } from 'react';
import { Button, Card, Hero } from '../components/ui';
import { api } from '../api';
import { Link } from 'react-router-dom';
import RepoModal from '../components/RepoModal';
import TemplateModal from '../components/TemplateModal';
import DockerImagesSection from '../components/DockerImagesSection';
import { shortDigest } from '../formatters';

export default function DashboardView() {
  const [repos, setRepos] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [images, setImages] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [editRepoData, setEditRepoData] = useState(null);
  const [editTemplateData, setEditTemplateData] = useState(null);
  const [isAddingRepo, setIsAddingRepo] = useState(false);
  const [isAddingTemplate, setIsAddingTemplate] = useState(false);

  const fetchData = async () => {
    try {
      const [reposData, templatesData, imagesData] = await Promise.all([
        api.getRepos(),
        api.getTemplates(),
        api.getDockerImages()
      ]);
      setRepos(reposData);
      setTemplates(templatesData);
      setImages(imagesData);
      setError('');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSaveRepo = async (payload, originalName) => {
    try {
      if (originalName) {
        await api.updateRepo(originalName, payload);
      } else {
        await api.createRepo(payload);
      }
      setEditRepoData(null);
      setIsAddingRepo(false);
      fetchData();
    } catch (err) {
      alert(err.message);
    }
  };

  const handleSaveTemplate = async (payload, originalName) => {
    try {
      if (originalName) {
        await api.updateTemplate(originalName, payload);
      } else {
        await api.createTemplate(payload);
      }
      setEditTemplateData(null);
      setIsAddingTemplate(false);
      fetchData();
    } catch (err) {
      alert(err.message);
    }
  };

  if (loading) {
    return <div className="container mt-xl text-center" style={{ color: 'var(--colors-mute)' }}>Loading dashboard...</div>;
  }

  if (error) {
    return <div className="container mt-xl"><p role="alert" className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>{error}</p><Button variant="ghost-sm" onClick={fetchData}>Retry</Button></div>;
  }

  if (repos.length === 0 && images.length === 0) {
    return <>
      <Hero />
      <section className="container mt-xl mb-xl">
        <h2 className="heading-lg mb-lg">Getting Started</h2>
        <div className="grid grid-cols-2 gap-lg">
          <Card>
            <p className="mono-eyebrow mb-sm">Step 01</p>
            <h3 className="heading-md mb-xs">Create a Jira template</h3>
            <p className="body-md text-mute mb-md">Define the ticket details for your tracked releases.</p>
            <Link to="/templates" className="button-primary-sm">Manage Templates</Link>
          </Card>
          <Card>
            <p className="mono-eyebrow mb-sm">Step 02</p>
            <h3 className="heading-md mb-xs">Start tracking releases</h3>
            <p className="body-md text-mute mb-md">Add a GitHub repository or standalone Docker image and link a template.</p>
            <Link to="/repos" className="button-primary-sm">Add Repository or Docker Image</Link>
          </Card>
        </div>
      </section>
    </>;
  }

  // Otherwise, show the populated dashboard
  return (
    <div>
      <div className="hero-band flex-col items-center justify-center" style={{ padding: 'var(--spacing-3xl) var(--spacing-lg)' }}>
        <h1 className="display-xl mb-md">Dashboard</h1>
        <p className="body-lg" style={{ color: 'var(--colors-body)' }}>
          Overview of your tracked repositories, Docker images, and templates.
        </p>
      </div>
      
      <div className="container mt-xl mb-xl">
        <div className="section-header mb-lg">
          <h2 className="heading-lg">Active Repositories</h2>
          <Link to="/repos" className="button-primary-sm section-header-action">Manage Repositories</Link>
        </div>
        
        <div className="grid grid-cols-6 gap-lg mb-xl">
          {repos.map(repo => (
            <div key={repo.repo} className="col-span-2" role="button" tabIndex={0} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setEditRepoData(repo); } }} onClick={() => setEditRepoData(repo)} style={{ display: 'block', minWidth: 0 }}>
              <Card className="flex-col" style={{ height: '100%', cursor: 'pointer' }}>
                <div className="flex justify-between items-center mb-sm">
                  <h3 className="heading-md">{repo.repo}</h3>
                  <span className="mono-eyebrow token-badge">
                    {repo.latest_tag || 'No releases'}
                  </span>
                </div>
                <p className="body-md mb-md" style={{ color: 'var(--colors-mute)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={repo.repo_url}>
                  {repo.repo_url.replace('https://api.github.com/repos/', '').replace('/releases/latest', '')}
                </p>
                {repo.docker_image && <p className="body-sm mb-md" title={repo.docker_digest}>Docker: {repo.docker_image}:latest · {shortDigest(repo.docker_digest)}</p>}
                <div className="mt-auto pt-md" style={{ borderTop: '1px solid var(--colors-hairline)' }}>
                  <span className="label-sm" style={{ color: 'var(--colors-mute)' }}>Linked Template: </span>
                  <span className="body-sm">{repo.template_name || 'None'}</span>
                </div>
              </Card>
            </div>
          ))}
          <div className="col-span-1" role="button" tabIndex={0} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setIsAddingRepo(true); } }} onClick={() => setIsAddingRepo(true)} style={{ display: 'block', minWidth: 0 }}>
            <Card className="flex-col items-center justify-center text-center" style={{ height: '100%', cursor: 'pointer', border: '1px dashed var(--colors-mute)', backgroundColor: 'transparent', minHeight: '120px' }}>
              <div className="heading-lg" style={{ color: 'var(--colors-mute)' }}>+</div>
              <p className="body-md mt-xs" style={{ color: 'var(--colors-mute)' }}>Add Repository</p>
            </Card>
          </div>
        </div>

        <DockerImagesSection templates={templates} layout="grid" images={images} onImagesChange={setImages} />

        <div className="section-header mb-lg">
          <h2 className="heading-lg">Templates Summary</h2>
          <Link to="/templates" className="button-primary-sm section-header-action">Manage Templates</Link>
        </div>
        
        <div className="grid grid-cols-6 gap-md mb-xl">
          {templates.slice(0, 3).map(template => (
            <div key={template.name} className="col-span-2" role="button" tabIndex={0} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setEditTemplateData(template); } }} onClick={() => setEditTemplateData(template)} style={{ display: 'block', minWidth: 0 }}>
              <Card className="flex-col" style={{ height: '100%', cursor: 'pointer' }}>
                <h3 className="heading-md mb-xs">{template.name}</h3>
                <p className="body-sm mb-sm" style={{ color: 'var(--colors-mute)' }}>Project: {template.project_key}</p>
                <p className="body-sm" style={{ color: 'var(--colors-mute)' }}>Type: {template.issue_type}</p>
              </Card>
            </div>
          ))}
          {templates.length === 0 && (
            <div className="col-span-6" style={{ textAlign: 'center', padding: 'var(--spacing-xl)', color: 'var(--colors-mute)', border: '1px dashed var(--colors-hairline)', borderRadius: 'var(--rounded-md)' }}>
              No templates found. <Link to="/templates" style={{ color: 'var(--colors-link)', textDecoration: 'none' }}>Create one now.</Link>
            </div>
          )}
          <div className="col-span-1" role="button" tabIndex={0} onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); setIsAddingTemplate(true); } }} onClick={() => setIsAddingTemplate(true)} style={{ display: 'block', minWidth: 0 }}>
            <Card className="flex-col items-center justify-center text-center" style={{ height: '100%', cursor: 'pointer', border: '1px dashed var(--colors-mute)', backgroundColor: 'transparent', minHeight: '120px' }}>
              <div className="heading-lg" style={{ color: 'var(--colors-mute)' }}>+</div>
              <p className="body-md mt-xs" style={{ color: 'var(--colors-mute)' }}>Add Template</p>
            </Card>
          </div>
        </div>
      </div>
      
      <RepoModal 
        isOpen={!!editRepoData || isAddingRepo} 
        onClose={() => { setEditRepoData(null); setIsAddingRepo(false); }} 
        onSave={handleSaveRepo}
        templates={templates}
        initialData={editRepoData}
      />
      <TemplateModal 
        isOpen={!!editTemplateData || isAddingTemplate} 
        onClose={() => { setEditTemplateData(null); setIsAddingTemplate(false); }} 
        onSave={handleSaveTemplate}
        initialData={editTemplateData}
      />
    </div>
  );
}
