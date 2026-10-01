import React, { useState, useEffect } from 'react';
import { api } from '../api';
import { Button, Input, Select, Card, Modal } from '../components/ui';

export default function TemplatesView() {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTemplateName, setEditingTemplateName] = useState(null);

  // Form state
  const [name, setName] = useState('');
  const [projectKey, setProjectKey] = useState('');
  const [issueType, setIssueType] = useState('Task');
  const [summary, setSummary] = useState('');
  const [description, setDescription] = useState('');
  const [assigneeId, setAssigneeId] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [priority, setPriority] = useState('High');
  const [labels, setLabels] = useState('');
  const [transition, setTransition] = useState('');

  const fetchTemplates = async () => {
    setLoading(true);
    try {
      const data = await api.getTemplates();
      setTemplates(data);
      setError(null);
      return data;
    } catch (err) {
      setError(err.message);
      return [];
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTemplates().then((data) => {
      const editName = new URLSearchParams(window.location.search).get('edit');
      if (editName && data && data.length > 0) {
        const tpl = data.find(t => t.name === editName);
        if (tpl) {
          handleEditClick(tpl);
        }
      }
    });
  }, []);

  const handleDelete = async (templateName) => {
    try {
      await api.deleteTemplate(templateName);
      fetchTemplates();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleTest = async (templateName) => {
    try {
      const res = await api.testTemplate(templateName);
      alert(res.message);
    } catch (err) {
      alert("Error: " + err.message);
    }
  };

  const handleCreateOrUpdate = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        name,
        project_key: projectKey,
        issue_type: issueType,
        summary,
        description,
        assignee_id: assigneeId || null,
        due_date: dueDate || null,
        priority,
        labels,
        transition
      };

      if (editingTemplateName) {
        await api.updateTemplate(editingTemplateName, payload);
      } else {
        await api.createTemplate(payload);
      }

      setIsModalOpen(false);
      setEditingTemplateName(null);
      setName('');
      setProjectKey('');
      setIssueType('Task');
      setSummary('');
      setDescription('');
      setAssigneeId('');
      setDueDate('');
      setLabels('');
      setTransition('');
      fetchTemplates();
    } catch (err) {
      alert(err.message);
    }
  };

  function handleEditClick(tpl) {
    setEditingTemplateName(tpl.name);
    setName(tpl.name);
    setProjectKey(tpl.project_key);
    setIssueType(tpl.issue_type);
    setSummary(tpl.summary);
    setDescription(tpl.description || '');
    setAssigneeId(tpl.assignee_id || '');
    setDueDate(tpl.due_date || '');
    setPriority(tpl.priority || 'High');
    setLabels(tpl.labels || '');
    setTransition(tpl.transition_name || '');
    setIsModalOpen(true);
  }

  function handleCreateClick() {
    setEditingTemplateName(null);
    setName('');
    setProjectKey('');
    setIssueType('Task');
    setSummary('');
    setDescription('');
    setAssigneeId('');
    setDueDate('');
    setPriority('High');
    setLabels('');
    setTransition('');
    setIsModalOpen(true);
  }

  return (
    <div className="container mt-xl">
      <div className="flex justify-between items-center mb-lg flex-col-mobile gap-sm">
        <div>
          <h1 className="heading-lg">Jira Templates</h1>
          <p className="body-md text-mute">Manage issue blueprints for tracked releases.</p>
        </div>
        <Button variant="primary" onClick={handleCreateClick}>Create Template</Button>
      </div>

      {error && <div className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>Error: {error}</div>}

      {loading ? (
        <div className="body-md">Loading...</div>
      ) : templates.length === 0 ? (
        <Card className="text-center py-xl">
          <p className="body-lg mb-sm">No templates found.</p>
          <Button variant="secondary" onClick={handleCreateClick}>Create your first template</Button>
        </Card>
      ) : (
        <div className="grid grid-cols-2 gap-md">
          {templates.map(tpl => (
            <Card key={tpl.name} elevated>
              <div className="flex justify-between items-center mb-sm">
                <h3 className="heading-md">{tpl.name}</h3>
                <span className="mono-eyebrow">{tpl.project_key}</span>
              </div>
              <div className="body-sm mb-xs"><strong>Summary:</strong> {tpl.summary}</div>
              {tpl.description && <div className="body-sm mb-xs" style={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}><strong>Description:</strong> {tpl.description}</div>}
              {tpl.assignee_id && <div className="body-sm mb-xs"><strong>Assignee ID:</strong> {tpl.assignee_id}</div>}
              {tpl.due_date && <div className="body-sm mb-xs"><strong>Due in:</strong> {tpl.due_date} days</div>}
              <div className="flex gap-sm mt-md">
                <span className="body-sm text-mute border px-xs rounded-sm">{tpl.issue_type}</span>
                {tpl.priority && <span className="body-sm text-mute border px-xs rounded-sm">{tpl.priority}</span>}
              </div>
              <div className="mt-md flex justify-end gap-sm">
                <Button variant="ghost-sm" onClick={() => handleEditClick(tpl)}>Edit</Button>
                <Button variant="ghost-sm" onClick={() => handleTest(tpl.name)}>Test Message</Button>
                <Button variant="danger" onClick={() => handleDelete(tpl.name)}>Delete</Button>
              </div>
            </Card>
          ))}
        </div>
      )}

      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title={editingTemplateName ? "Edit Template" : "Create Template"}>
        <form onSubmit={handleCreateOrUpdate} className="flex-col gap-sm">
          <div>
            <label className="label-sm mb-xs display-block">Name (e.g., ops-upgrade)</label>
            <Input value={name} onChange={e => setName(e.target.value)} required />
          </div>
          <div>
            <label className="label-sm mb-xs display-block">Project Key (e.g., OPS)</label>
            <Input value={projectKey} onChange={e => setProjectKey(e.target.value)} required />
          </div>
          <div>
            <label className="label-sm mb-xs display-block">Issue Type</label>
            <Select value={issueType} onChange={e => setIssueType(e.target.value)}>
              <option value="Task">Task</option>
              <option value="Bug">Bug</option>
              <option value="Story">Story</option>
            </Select>
          </div>
          <div>
            <label className="label-sm mb-xs display-block">Summary (Supports {`{repo}, {tag}, {url}`})</label>
            <Input value={summary} onChange={e => setSummary(e.target.value)} required />
          </div>
          <div>
            <label className="label-sm mb-xs display-block">Description</label>
            <textarea 
              className="text-input" 
              style={{ minHeight: '80px', resize: 'vertical' }}
              value={description} 
              onChange={e => setDescription(e.target.value)} 
            />
          </div>
          <div className="grid grid-cols-2 gap-sm">
            <div>
              <label className="label-sm mb-xs display-block">Assignee ID</label>
              <Input value={assigneeId} onChange={e => setAssigneeId(e.target.value)} placeholder="e.g., 5b12345..." />
            </div>
            <div>
              <label className="label-sm mb-xs display-block">Due Date (Days)</label>
              <Input value={dueDate} onChange={e => setDueDate(e.target.value)} placeholder="e.g., 3" type="number" min="0" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-sm">
            <div>
              <label className="label-sm mb-xs display-block">Priority</label>
              <Select value={priority} onChange={e => setPriority(e.target.value)}>
                <option value="Highest">Highest</option>
                <option value="High">High</option>
                <option value="Medium">Medium</option>
                <option value="Low">Low</option>
                <option value="Lowest">Lowest</option>
              </Select>
            </div>
            <div>
              <label className="label-sm mb-xs display-block">Transition</label>
              <Input value={transition} onChange={e => setTransition(e.target.value)} placeholder="e.g., In Progress" />
            </div>
          </div>
          <div>
            <label className="label-sm mb-xs display-block">Labels (comma-separated)</label>
            <Input value={labels} onChange={e => setLabels(e.target.value)} />
          </div>
          <div className="flex justify-end gap-sm mt-md">
            <Button variant="ghost-sm" type="button" onClick={() => setIsModalOpen(false)}>Cancel</Button>
            <Button variant="primary" type="submit">{editingTemplateName ? "Save Changes" : "Create"}</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
