import { Grid, Card, CardContent, CardActionArea, Typography, Chip, Stack, Box } from '@mui/material'
import { POLICY_TEMPLATES, PolicyTemplate } from '../../data/policyTemplates'

interface Props {
  onSelect: (template: PolicyTemplate) => void
}

export default function TemplateSelectionStep({ onSelect }: Props) {
  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 1 }}>
        Choose a starting point
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Select a template to pre-populate your policy with proven rules, or start from scratch.
      </Typography>

      <Grid container spacing={2}>
        {POLICY_TEMPLATES.map((template) => (
          <Grid item xs={12} sm={6} key={template.id}>
            <Card
              variant="outlined"
              sx={{
                height: '100%',
                borderStyle: template.dashed ? 'dashed' : 'solid',
                borderColor: template.dashed ? 'rgba(139, 148, 158, 0.3)' : 'rgba(139, 148, 158, 0.1)',
                transition: 'border-color 0.2s, box-shadow 0.2s',
                '&:hover': {
                  borderColor: template.accentColor,
                  boxShadow: `0 0 20px ${template.accentColor}22`,
                },
              }}
            >
              <CardActionArea onClick={() => onSelect(template)} sx={{ height: '100%' }}>
                <CardContent sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
                  <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 1 }}>
                    <Chip
                      label={template.iconLabel}
                      size="small"
                      sx={{
                        bgcolor: `${template.accentColor}22`,
                        color: template.accentColor,
                        fontWeight: 700,
                        fontSize: '0.7rem',
                      }}
                    />
                  </Stack>

                  <Typography variant="h6" sx={{ mb: 0.5, fontSize: '1.1rem' }}>
                    {template.name}
                  </Typography>

                  <Typography variant="body2" color="text.secondary" sx={{ mb: 2, flexGrow: 1 }}>
                    {template.description}
                  </Typography>

                  {!template.dashed && (
                    <Stack direction="row" spacing={0.5} flexWrap="wrap">
                      {template.severitySummary.FAIL > 0 && (
                        <Chip label={`${template.severitySummary.FAIL} Block`} size="small" color="error" variant="outlined" sx={{ fontSize: '0.7rem' }} />
                      )}
                      {template.severitySummary.WARN > 0 && (
                        <Chip label={`${template.severitySummary.WARN} Warn`} size="small" color="warning" variant="outlined" sx={{ fontSize: '0.7rem' }} />
                      )}
                      {template.severitySummary.INFO > 0 && (
                        <Chip label={`${template.severitySummary.INFO} Info`} size="small" color="info" variant="outlined" sx={{ fontSize: '0.7rem' }} />
                      )}
                      <Chip
                        label={`${template.form.rules.length} rule${template.form.rules.length !== 1 ? 's' : ''}`}
                        size="small"
                        variant="outlined"
                        sx={{ fontSize: '0.7rem', color: 'text.secondary', borderColor: 'rgba(139,148,158,0.3)' }}
                      />
                    </Stack>
                  )}
                </CardContent>
              </CardActionArea>
            </Card>
          </Grid>
        ))}
      </Grid>
    </Box>
  )
}
