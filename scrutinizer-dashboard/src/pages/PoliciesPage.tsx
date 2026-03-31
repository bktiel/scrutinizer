import { useEffect, useState, useRef } from 'react'
import {
  Typography, Box, Button, Card, CardContent, CardActions,
  IconButton, Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Stack, Chip, Alert
} from '@mui/material'
import DeleteIcon from '@mui/icons-material/Delete'
import UploadFileIcon from '@mui/icons-material/UploadFile'
import HistoryIcon from '@mui/icons-material/History'
import {
  listPolicies, uploadPolicy, deletePolicy, getPolicyHistory,
  Policy, PolicyHistory
} from '../api/scrutinizerApi'

export default function PoliciesPage() {
  const [policies, setPolicies] = useState<Policy[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [historyData, setHistoryData] = useState<PolicyHistory[]>([])
  const [historyPolicyName, setHistoryPolicyName] = useState('')
  const [description, setDescription] = useState('')
  const fileRef = useRef<HTMLInputElement>(null)

  const refresh = () => {
    setLoading(true)
    listPolicies()
      .then(setPolicies)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }

  useEffect(refresh, [])

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0]
    if (!file) return
    try {
      setError(null)
      await uploadPolicy(file, description || undefined)
      setUploadOpen(false)
      setDescription('')
      refresh()
    } catch (e: any) {
      setError(e.message)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this policy? Runs using it will retain their results.')) return
    try {
      await deletePolicy(id)
      refresh()
    } catch (e: any) {
      setError(e.message)
    }
  }

  const handleHistory = async (policy: Policy) => {
    try {
      const data = await getPolicyHistory(policy.id)
      setHistoryData(data)
      setHistoryPolicyName(policy.name)
      setHistoryOpen(true)
    } catch (e: any) {
      setError(e.message)
    }
  }

  return (
    <Box>
      <Stack direction="row" justifyContent="space-between" alignItems="center" sx={{ mb: 3 }}>
        <Typography variant="h4">Policies</Typography>
        <Button variant="contained" startIcon={<UploadFileIcon />} onClick={() => setUploadOpen(true)}>
          Upload Policy
        </Button>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      {!loading && policies.length === 0 && (
        <Typography color="text.secondary">No policies yet. Upload a YAML policy file to get started.</Typography>
      )}

      <Stack spacing={2}>
        {policies.map((p) => (
          <Card key={p.id} variant="outlined">
            <CardContent>
              <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                <Box>
                  <Typography variant="h6">{p.name}</Typography>
                  <Stack direction="row" spacing={1} sx={{ mt: 0.5 }}>
                    <Chip label={`v${p.version}`} size="small" variant="outlined" />
                    <Chip label={p.id.slice(0, 8)} size="small" color="default" variant="outlined" />
                  </Stack>
                  {p.description && (
                    <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                      {p.description}
                    </Typography>
                  )}
                  <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                    Updated {new Date(p.updatedAt).toLocaleString()}
                  </Typography>
                </Box>
              </Stack>
            </CardContent>
            <CardActions>
              <IconButton size="small" onClick={() => handleHistory(p)} title="View history">
                <HistoryIcon />
              </IconButton>
              <IconButton size="small" color="error" onClick={() => handleDelete(p.id)} title="Delete">
                <DeleteIcon />
              </IconButton>
            </CardActions>
          </Card>
        ))}
      </Stack>

      <Dialog open={uploadOpen} onClose={() => setUploadOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Upload Policy YAML</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <input ref={fileRef} type="file" accept=".yaml,.yml" />
            <TextField
              label="Description (optional)"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              fullWidth
              size="small"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setUploadOpen(false)}>Cancel</Button>
          <Button variant="contained" onClick={handleUpload}>Upload</Button>
        </DialogActions>
      </Dialog>

      <Dialog open={historyOpen} onClose={() => setHistoryOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>History: {historyPolicyName}</DialogTitle>
        <DialogContent>
          {historyData.length === 0 ? (
            <Typography color="text.secondary">No history entries.</Typography>
          ) : (
            <Stack spacing={2} sx={{ mt: 1 }}>
              {historyData.map((h) => (
                <Card key={h.id} variant="outlined">
                  <CardContent>
                    <Typography variant="caption" color="text.secondary">
                      {new Date(h.changedAt).toLocaleString()} {h.changedBy && `by ${h.changedBy}`}
                    </Typography>
                    <Box component="pre" sx={{ mt: 1, fontSize: 12, maxHeight: 200, overflow: 'auto', bgcolor: 'grey.50', p: 1, borderRadius: 1 }}>
                      {h.policyYaml}
                    </Box>
                  </CardContent>
                </Card>
              ))}
            </Stack>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setHistoryOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}
