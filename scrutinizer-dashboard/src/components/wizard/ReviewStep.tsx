import { Box, Typography, Card, CardContent, Stack, Chip, Accordion, AccordionSummary, AccordionDetails } from '@mui/material'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import type { PolicyForm } from '../../utils/policyYaml'
import { generatePolicyYaml } from '../../utils/policyYaml'
import ImpactPreview from './ImpactPreview'

interface Props {
  form: PolicyForm
}

export default function ReviewStep({ form }: Props) {
  const severityCounts = { FAIL: 0, WARN: 0, INFO: 0, SKIP: 0 }
  for (const rule of form.rules) {
    severityCounts[rule.severity]++
  }

  const yaml = generatePolicyYaml(form)

  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 1 }}>
        Review & Save
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Review your policy before saving. Check the impact preview to see how it would evaluate real packages.
      </Typography>

      {/* Summary Card */}
      <Card variant="outlined" sx={{ mb: 3 }}>
        <CardContent>
          <Stack direction="row" justifyContent="space-between" alignItems="flex-start" flexWrap="wrap" gap={2}>
            <Box>
              <Typography variant="h6" sx={{ fontSize: '1.1rem' }}>
                {form.name || '(Unnamed policy)'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Version {form.version}
                {form.description && ` \u2014 ${form.description}`}
              </Typography>
            </Box>
            <Stack direction="row" spacing={0.5} flexWrap="wrap">
              <Chip
                label={`${form.rules.length} rule${form.rules.length !== 1 ? 's' : ''}`}
                size="small"
                variant="outlined"
                sx={{ fontWeight: 600 }}
              />
              {severityCounts.FAIL > 0 && (
                <Chip label={`${severityCounts.FAIL} Block`} size="small" color="error" variant="outlined" />
              )}
              {severityCounts.WARN > 0 && (
                <Chip label={`${severityCounts.WARN} Warn`} size="small" color="warning" variant="outlined" />
              )}
              {severityCounts.INFO > 0 && (
                <Chip label={`${severityCounts.INFO} Info`} size="small" color="info" variant="outlined" />
              )}
              {severityCounts.SKIP > 0 && (
                <Chip label={`${severityCounts.SKIP} Disabled`} size="small" variant="outlined" />
              )}
            </Stack>
          </Stack>
        </CardContent>
      </Card>

      {/* Impact Preview */}
      <Box sx={{ mb: 3 }}>
        <ImpactPreview form={form} />
      </Box>

      {/* YAML Preview */}
      <Accordion
        variant="outlined"
        sx={{
          '&:before': { display: 'none' },
          borderColor: 'rgba(139, 148, 158, 0.1)',
        }}
      >
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
            Generated YAML
          </Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Box
            component="pre"
            sx={{
              p: 2,
              borderRadius: 1,
              bgcolor: '#0D1117',
              overflow: 'auto',
              maxHeight: 400,
              fontSize: '0.8rem',
              fontFamily: 'monospace',
              color: '#C9D1D9',
              m: 0,
            }}
          >
            {yaml}
          </Box>
        </AccordionDetails>
      </Accordion>
    </Box>
  )
}
