import React, { useState, useEffect } from 'react';
import { Hero, Button, Card } from '../components/ui';
import { api } from '../api';
import { Link } from 'react-router-dom';
import RepoModal from '../components/RepoModal';
import TemplateModal from '../components/TemplateModal';

export default function DashboardView() {
  const [repos, setRepos] = useState([]);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editRepoData, setEditRepoData] = useState(null);
  const [editTemplateData, setEditTemplateData] = useState(null);
  const [isAddingRepo, setIsAddingRepo] = useState(false);
  const [isAddingTemplate, setIsAddingTemplate] = useState(false);

  const fetchData = async () => {
    try {
      const [reposData, templatesData] = await Promise.all([
        api.getRepos(),
        api.getTemplates()
      ]);
      setRepos(reposData);
      setTemplates(templatesData);
    } catch (err) {
      console.error("Failed to fetch dashboard data", err);
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
      fetchData();
    } catch (err) {
      alert(err.message);
    }
  };

  if (loading) {
    return <div className="container mt-xl text-center" style={{ color: 'var(--colors-mute)' }}>Loading dashboard...</div>;
  }

  // If no repos exist, show the getting started view
  if (repos.length === 0) {
    return (
      <div>
        <Hero />
        <div className="container mt-xl">
          <h2 className="heading-lg mb-md text-center">Getting Started</h2>
          <div className="grid grid-cols-2 gap-lg" style={{ maxWidth: '800px', margin: '0 auto' }}>
            <div className="feature-card flex-col items-center justify-center text-center">
              <h3 className="heading-md mb-xs">1. Create a Template</h3>
              <p className="body-md mb-md" style={{ color: 'var(--colors-body)' }}>Define the Jira ticket structure you want to create when a new release is detected.</p>
              <Link to="/templates"><Button variant="primary-sm">Go to Templates</Button></Link>
            </div>
            <div className="feature-card flex-col items-center justify-center text-center">
              <h3 className="heading-md mb-xs">2. Track a Repository</h3>
              <p className="body-md mb-md" style={{ color: 'var(--colors-body)' }}>Link a GitHub repository to your template and let Release Tracker do the rest.</p>
              <Link to="/repos"><Button variant="primary-sm">Go to Repositories</Button></Link>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Otherwise, show the populated dashboard
  return (
    <div>
      <div className="hero-band flex-col items-center justify-center" style={{ padding: 'var(--spacing-3xl) var(--spacing-lg)' }}>
        <h1 className="display-xl mb-md">Dashboard</h1>
        <p className="body-lg" style={{ color: 'var(--colors-body)' }}>
          Overview of your tracked repositories and templates.
        </p>
      </div>
      
      <div className="container mt-xl mb-xl">
        <div className="flex justify-between items-center mb-lg flex-col-mobile gap-sm">
          <h2 className="heading-lg">Active Repositories</h2>
          <Link to="/repos"><Button variant="primary-sm">Manage Repositories</Button></Link>
        </div>
        
        <div className="grid grid-cols-6 gap-lg mb-xl">
          {repos.map(repo => (
            <div key={repo.repo} className="col-span-2" onClick={() => setEditRepoData(repo)} style={{ display: 'block', minWidth: 0 }}>
              <Card className="flex-col" style={{ height: '100%', cursor: 'pointer' }}>
                <div className="flex justify-between items-center mb-sm">
                  <h3 className="heading-md">{repo.repo}</h3>
                  <span className="mono-eyebrow" style={{ color: 'var(--colors-link)', backgroundColor: 'var(--colors-link-soft)', padding: '2px 6px', borderRadius: '4px' }}>
                    {repo.latest_tag || 'No releases'}
                  </span>
                </div>
                <p className="body-md mb-md" style={{ color: 'var(--colors-mute)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={repo.repo_url}>
                  {repo.repo_url.replace('https://api.github.com/repos/', '').replace('/releases/latest', '')}
                </p>
                <div className="mt-auto pt-md" style={{ borderTop: '1px solid var(--colors-hairline)' }}>
                  <span className="label-sm" style={{ color: 'var(--colors-mute)' }}>Linked Template: </span>
                  <span className="body-sm">{repo.template_name || 'None'}</span>
                </div>
              </Card>
            </div>
          ))}
          <div className="col-span-1" onClick={() => setIsAddingRepo(true)} style={{ display: 'block', minWidth: 0 }}>
            <Card className="flex-col items-center justify-center text-center" style={{ height: '100%', cursor: 'pointer', border: '1px dashed var(--colors-mute)', backgroundColor: 'transparent', minHeight: '120px' }}>
              <div className="heading-lg" style={{ color: 'var(--colors-mute)' }}>+</div>
              <p className="body-md mt-xs" style={{ color: 'var(--colors-mute)' }}>Add Repository</p>
            </Card>
          </div>
        </div>

        <div className="flex justify-between items-center mb-lg flex-col-mobile gap-sm">
          <h2 className="heading-lg">Templates Summary</h2>
          <Link to="/templates"><Button variant="primary-sm">Manage Templates</Button></Link>
        </div>
        
        <div className="grid grid-cols-6 gap-md mb-xl">
          {templates.slice(0, 3).map(template => (
            <div key={template.id} className="col-span-2" onClick={() => setEditTemplateData(template)} style={{ display: 'block', minWidth: 0 }}>
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
          <div className="col-span-1" onClick={() => setIsAddingTemplate(true)} style={{ display: 'block', minWidth: 0 }}>
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
