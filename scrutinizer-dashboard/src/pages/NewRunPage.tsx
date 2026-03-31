import { useEffect, useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Typography, Box, Button, TextField, Stack, Alert,
  FormControl, InputLabel, Select, MenuItem
} from '@mui/material'
import PlayArrowIcon from '@mui/icons-material/PlayArrow'
import { listPolicies, createRun, Policy } from '../api/scrutinizerApi'

export default function NewRunPage() {
  const navigate = useNavigate()
  const [policies, setPolicies] = useState<Policy[]>([])
  const [selectedPolicy, setSelectedPolicy] = useState('')
  const [applicationName, setApplicationName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [running, setRunning] = useState(false)
  const fileRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    listPolicies().then(setPolicies).catch((e) => setError(e.message))
  }, [])

  const handleSubmit = async () => {
    const file = fileRef.current?.files?.[0]
    if (!file) { setError('Please select an SBOM file'); return }
    if (!applicationName.trim()) { setError('Please enter an application name'); return }
    if (!selectedPolicy) { setError('Please select a policy'); return }

    setRunning(true)
    setError(null)
    try {
      const result = await createRun(file, applicationName.trim(), selectedPolicy)
      navigate(`/runs/${result.id}`)
    } catch (e: any) {
      setError(e.message)
      setRunning(false)
    }
  }

  return (
    <Box maxWidth={600}>
      <Typography variant="h4" gutterBottom>New Posture Evaluation</Typography>

      {error && <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>{error}</Alert>}

      <Stack spacing={3}>
        <TextField
          label="Application Name"
          value={applicationName}
          onChange={(e) => setApplicationName(e.target.value)}
          fullWidth
          placeholder="e.g. my-service"
        />

        <FormControl fullWidth>
          <InputLabel>Policy</InputLabel>
          <Select
            value={selectedPolicy}
            label="Policy"
            onChange={(e) => setSelectedPolicy(e.target.value)}
          >
            {policies.map((p) => (
              <MenuItem key={p.id} value={p.id}>
                {p.name} (v{p.version})
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        <Box>
          <Typography variant="subtitle2" gutterBottom>CycloneDX SBOM (JSON)</Typography>
          <input ref={fileRef} type="file" accept=".json" />
        </Box>

        <Button
          variant="contained"
          size="large"
          startIcon={<PlayArrowIcon />}
          onClick={handleSubmit}
          disabled={running}
        >
          {running ? 'Evaluating...' : 'Run Evaluation'}
        </Button>
      </Stack>
    </Box>
  )
}
