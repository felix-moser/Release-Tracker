export const api = {
  async getRepos() {
    const res = await fetch('/api/repos');
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  async createRepo(repo) {
    const res = await fetch('/api/repos', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(repo)
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || 'Failed to create repository');
    }
    return res.json();
  },
  async updateRepo(originalName, repo) {
    const res = await fetch(`/api/repos/${encodeURIComponent(originalName)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(repo)
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || 'Failed to update repository');
    }
    return res.json();
  },
  async deleteRepo(name) {
    const res = await fetch(`/api/repos/${encodeURIComponent(name)}`, {
      method: 'DELETE'
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async getTemplates() {
    const res = await fetch('/api/templates');
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  async createTemplate(template) {
    const res = await fetch('/api/templates', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(template)
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || 'Failed to create template');
    }
    return res.json();
  },
  async updateTemplate(originalName, template) {
    const res = await fetch(`/api/templates/${encodeURIComponent(originalName)}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(template)
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || 'Failed to update template');
    }
    return res.json();
  },
  async deleteTemplate(name) {
    const res = await fetch(`/api/templates/${encodeURIComponent(name)}`, {
      method: 'DELETE'
    });
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  async testTemplate(name) {
    const res = await fetch(`/api/templates/${encodeURIComponent(name)}/test`, {
      method: 'POST'
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || 'Failed to send test message');
    }
    return res.json();
  },

  async checkOnboarding() {
    const res = await fetch('/api/onboarded');
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  
  async saveSecrets(secrets) {
    const res = await fetch('/api/secrets', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(secrets)
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || 'Failed to save secrets');
    }
    return res.json();
  },

  async getSecrets() {
    const res = await fetch('/api/secrets');
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },

  async getSchedulerStatus() {
    const res = await fetch('/api/scheduler/status');
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  },
  
  async updateSchedulerInterval(interval_minutes) {
    const res = await fetch('/api/scheduler/interval', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ interval_minutes })
    });
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      throw new Error(errorData.detail || 'Failed to update interval');
    }
    return res.json();
  }
};
