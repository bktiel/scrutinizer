import { Box, Stack, TextField, Typography } from '@mui/material'
import type { PolicyForm } from '../../utils/policyYaml'

interface Props {
  form: PolicyForm
  onChange: (form: PolicyForm) => void
}

export default function MetadataStep({ form, onChange }: Props) {
  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 1 }}>
        Policy Details
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Give your policy a name and description so your team knows what it does.
      </Typography>

      <Stack spacing={2.5}>
        <TextField
          label="Policy Name"
          value={form.name}
          onChange={(e) => onChange({ ...form, name: e.target.value })}
          fullWidth
          required
          placeholder="my-security-policy"
          helperText="A short, descriptive name. Used in CI pipelines and API calls."
        />

        <TextField
          label="Version"
          value={form.version}
          onChange={(e) => onChange({ ...form, version: e.target.value })}
          fullWidth
          placeholder="1.0"
          helperText="Use versioning to track policy changes over time (e.g. 1.0, 2.0)."
        />

        <TextField
          label="Description"
          value={form.description}
          onChange={(e) => onChange({ ...form, description: e.target.value })}
          fullWidth
          multiline
          rows={3}
          placeholder="Describe what this policy checks and why it matters to your team..."
          helperText="Helps your team understand the policy's purpose."
        />
      </Stack>
    </Box>
  )
}
