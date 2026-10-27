import React, { useEffect, useState } from 'react';
import { api } from '../api';
import { Button, Card, Input, Modal } from './ui';

export default function RegistryCredentials() {
  const [registries, setRegistries] = useState([]);
  const [form, setForm] = useState({ registry: '', username: '', token: '' });
  const [editingRegistry, setEditingRegistry] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [error, setError] = useState('');
  const [modalError, setModalError] = useState('');
  const [message, setMessage] = useState('');
  const [saving, setSaving] = useState(false);

  const load = async () => {
    try {
      setRegistries(await api.getRegistries());
      setError('');
    } catch (e) {
      setError(e.message);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const openCreateModal = () => {
    setEditingRegistry(null);
    setForm({ registry: '', username: '', token: '' });
    setModalError('');
    setIsModalOpen(true);
  };

  const openEditModal = (r) => {
    setEditingRegistry(r);
    setForm({ registry: r.registry, username: r.username, token: '' });
    setModalError('');
    setIsModalOpen(true);
  };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setModalError('');
    setMessage('');
    try {
      await api.saveRegistry(form);
      setIsModalOpen(false);
      setEditingRegistry(null);
      setForm({ registry: '', username: '', token: '' });
      await load();
      setMessage('Registry credentials saved.');
    } catch (e) {
      setModalError(e.message);
    } finally {
      setSaving(false);
    }
  };

  const remove = async (registry) => {
    setError('');
    setMessage('');
    try {
      await api.deleteRegistry(registry);
      await load();
      setMessage('Registry removed.');
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <Card>
      <div className="section-header mb-md">
        <div>
          <h2 className="heading-md mb-xs">Container Registry Credentials</h2>
          <p className="body-sm text-mute">
            Configure credentials for Docker Hub, GHCR, and other private registries.
          </p>
        </div>
        <Button variant="primary-sm" className="section-header-action" type="button" onClick={openCreateModal}>
          Add Registry
        </Button>
      </div>

      {error && (
        <p role="alert" className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>
          {error}
        </p>
      )}
      {message && (
        <p role="status" className="mb-md body-sm" style={{ color: 'var(--colors-link)' }}>
          {message}
        </p>
      )}

      {registries.length === 0 ? (
        <p className="body-sm text-mute">No container registries configured yet.</p>
      ) : (
        <div className="table-container">
          <table>
            <colgroup><col /><col /><col /><col className="table-actions-column" /></colgroup>
            <thead>
              <tr>
                <th>Registry</th>
                <th>Username</th>
                <th className="text-center">Token</th>
                <th className="table-actions">Actions</th>
              </tr>
            </thead>
            <tbody>
              {registries.map((r) => (
                <tr key={r.registry}>
                  <td><strong>{r.registry}</strong></td>
                  <td>{r.username}</td>
                  <td className="text-center">
                    <span
                      className="mono-eyebrow token-badge"
                      style={{ color: r.token_set ? 'var(--colors-link)' : 'var(--colors-mute)' }}
                    >
                      {r.token_set ? 'Configured' : 'Missing'}
                    </span>
                  </td>
                  <td className="table-actions">
                    <div className="flex justify-center gap-sm">
                      <Button variant="ghost-sm" type="button" onClick={() => openEditModal(r)}>
                        Edit
                      </Button>
                      <Button variant="danger" type="button" onClick={() => remove(r.registry)}>
                        Remove
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={editingRegistry ? 'Edit Registry Credential' : 'Add Registry Credential'}
      >
        <form className="flex-col gap-md" onSubmit={save}>
          {modalError && (
            <p role="alert" className="mb-md body-sm" style={{ color: 'var(--colors-error)' }}>
              {modalError}
            </p>
          )}
          <div className="flex-col gap-xs">
            <label htmlFor="registry-host" className="label-sm display-block">
              Registry host
            </label>
            <Input
              id="registry-host"
              placeholder="docker.io or ghcr.io"
              required
              value={form.registry}
              onChange={(e) => setForm({ ...form, registry: e.target.value })}
            />
          </div>
          <div className="flex-col gap-xs">
            <label htmlFor="registry-user" className="label-sm display-block">
              Username
            </label>
            <Input
              id="registry-user"
              required
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
            />
          </div>
          <div className="flex-col gap-xs">
            <label htmlFor="registry-token" className="label-sm display-block">
              Password or token
            </label>
            <Input
              id="registry-token"
              type="password"
              autoComplete="new-password"
              placeholder={
                editingRegistry?.token_set
                  ? '••••••••  (token is configured)'
                  : 'Enter password or access token'
              }
              required={!editingRegistry}
              value={form.token}
              onChange={(e) => setForm({ ...form, token: e.target.value })}
            />
            {editingRegistry?.token_set && (
              <p className="body-sm text-mute mt-xs">
                Leave blank to retain the existing token.
              </p>
            )}
          </div>
          <div className="flex justify-end gap-sm mt-md">
            <Button variant="ghost-sm" type="button" onClick={() => setIsModalOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" variant="primary-sm" disabled={saving}>
              {saving ? 'Saving...' : editingRegistry ? 'Save Changes' : 'Add'}
            </Button>
          </div>
        </form>
      </Modal>
    </Card>
  );
}
