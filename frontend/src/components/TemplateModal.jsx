import React, { useState, useEffect } from 'react';
import { Modal, Input, Select, Button } from './ui';

export default function TemplateModal({ isOpen, onClose, onSave, initialData }) {
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

  useEffect(() => {
    if (isOpen) {
      setName(initialData ? initialData.name : '');
      setProjectKey(initialData ? initialData.project_key : '');
      setIssueType(initialData ? initialData.issue_type : 'Task');
      setSummary(initialData ? initialData.summary : '');
      setDescription(initialData ? (initialData.description || '') : '');
      setAssigneeId(initialData ? (initialData.assignee_id || '') : '');
      setDueDate(initialData ? (initialData.due_date || '') : '');
      setPriority(initialData ? (initialData.priority || 'High') : 'High');
      setLabels(initialData ? (initialData.labels || '') : '');
      setTransition(initialData ? (initialData.transition_name || '') : '');
    }
  }, [isOpen, initialData]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({
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
    }, initialData ? initialData.name : null);
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={initialData ? "Edit Template" : "Create Template"}>
      <form onSubmit={handleSubmit} className="flex-col gap-sm">
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
          <Button variant="ghost-sm" type="button" onClick={onClose}>Cancel</Button>
          <Button variant="primary" type="submit">{initialData ? "Save Changes" : "Create"}</Button>
        </div>
      </form>
    </Modal>
  );
}
