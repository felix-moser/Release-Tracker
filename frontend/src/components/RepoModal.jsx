import React, { useState, useEffect } from 'react';
import { Modal, Input, Select, Button } from './ui';
import VersionComparisonField from './VersionComparisonField';

export default function RepoModal({ isOpen, onClose, onSave, templates, initialData }) {
  const [name, setName] = useState('');
  const [githubUrl, setGithubUrl] = useState('');
  const [template, setTemplate] = useState('');
  const [dockerImage, setDockerImage] = useState('');
  const [dockerTemplate, setDockerTemplate] = useState('');
  const [versionComparison, setVersionComparison] = useState('ANY_DIFFERENT');

  useEffect(() => {
    if (isOpen) {
      setVersionComparison(initialData?.version_comparison || 'ANY_DIFFERENT');
    }
  }, [isOpen, initialData]);

  useEffect(() => {
    if (isOpen) {
      setDockerImage(initialData?.docker_image || '');
      setDockerTemplate(initialData?.docker_template || '');
      setName(initialData ? initialData.repo : '');
      setGithubUrl(initialData ? initialData.repo_url : '');
      setTemplate(initialData ? (initialData.template_name || '') : (templates.length > 0 ? templates[0].name : ''));
    }
  }, [isOpen, initialData, templates]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({
      name,
      github_url: githubUrl,
      template: template || undefined,
      docker_image: dockerImage,
      docker_template: dockerTemplate || null,
      version_comparison: versionComparison
    }, initialData ? initialData.repo : null);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={initialData ? "Edit Repository" : "Add Repository"}>
      <form onSubmit={handleSubmit} className="flex-col gap-sm">
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
        <fieldset className="flex-col gap-sm border rounded-sm" style={{ padding: '12px' }}>
          <legend className="label-sm">Docker Image (optional)</legend>
          <label htmlFor="linked-docker-image" className="label-sm">Docker Image</label>
          <Input id="linked-docker-image" value={dockerImage} onChange={e => setDockerImage(e.target.value)} placeholder="nginx or ghcr.io/owner/app" />
          <p className="body-sm text-mute">Always tracks :latest. Any supplied tag or digest is stripped. Clear to unlink.</p>
          <label htmlFor="linked-docker-template" className="label-sm">Docker Jira Template</label>
          <Select id="linked-docker-template" value={dockerTemplate} onChange={e => setDockerTemplate(e.target.value)}>
            <option value="">(None)</option>
            {templates.map(t => <option key={t.name} value={t.name}>{t.name}</option>)}
          </Select>
        </fieldset>
        <VersionComparisonField value={versionComparison} onChange={setVersionComparison} />
        <div className="flex justify-end gap-sm mt-md">
          <Button variant="ghost-sm" type="button" onClick={onClose}>Cancel</Button>
          <Button variant="primary" type="submit">{initialData ? "Save Changes" : "Add"}</Button>
        </div>
      </form>
    </Modal>
  );
}
