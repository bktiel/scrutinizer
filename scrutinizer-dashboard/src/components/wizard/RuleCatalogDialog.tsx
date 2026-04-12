import {
  Dialog,
  DialogTitle,
  DialogContent,
  List,
  ListItemButton,
  ListItemText,
  Typography,
  Divider,
  Box,
} from '@mui/material'
import { RULE_CATALOG, CATEGORY_LABELS, CatalogEntry } from '../../data/ruleCatalog'

interface Props {
  open: boolean
  onClose: () => void
  onSelect: (entry: CatalogEntry) => void
}

export default function RuleCatalogDialog({ open, onClose, onSelect }: Props) {
  const categories = ['security', 'provenance', 'package-ban', 'metadata'] as const
  const grouped = categories.map((cat) => ({
    category: cat,
    label: CATEGORY_LABELS[cat],
    entries: RULE_CATALOG.filter((e) => e.category === cat),
  }))

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Add a Rule</DialogTitle>
      <DialogContent sx={{ px: 0, pb: 0 }}>
        <Typography variant="body2" color="text.secondary" sx={{ px: 3, mb: 2 }}>
          Choose a rule type to add to your policy. You can customize its parameters afterward.
        </Typography>

        {grouped.map((group, gi) => (
          <Box key={group.category}>
            {gi > 0 && <Divider />}
            <Typography
              variant="overline"
              sx={{ px: 3, pt: 1.5, pb: 0.5, display: 'block', color: 'text.secondary', letterSpacing: 1 }}
            >
              {group.label}
            </Typography>
            <List disablePadding>
              {group.entries.map((entry) => (
                <ListItemButton
                  key={entry.id}
                  onClick={() => {
                    onSelect(entry)
                    onClose()
                  }}
                  sx={{ px: 3, py: 1 }}
                >
                  <ListItemText
                    primary={entry.name}
                    secondary={entry.description}
                    primaryTypographyProps={{ fontWeight: 600, fontSize: '0.9rem' }}
                    secondaryTypographyProps={{ fontSize: '0.8rem' }}
                  />
                </ListItemButton>
              ))}
            </List>
          </Box>
        ))}
      </DialogContent>
    </Dialog>
  )
}
