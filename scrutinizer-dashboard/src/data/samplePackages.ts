export interface SamplePackage {
  name: string
  version: string
  type: string
  group: string
  purl: string
  scope: string
  'scorecard.score': number
  'provenance.present': boolean
  label: string
}

export const SAMPLE_PACKAGES: SamplePackage[] = [
  {
    name: 'express',
    version: '4.18.2',
    type: 'library',
    group: '',
    purl: 'pkg:npm/express@4.18.2',
    scope: 'compile',
    'scorecard.score': 7.2,
    'provenance.present': true,
    label: 'Well-maintained web framework',
  },
  {
    name: 'lodash',
    version: '4.17.21',
    type: 'library',
    group: '',
    purl: 'pkg:npm/lodash@4.17.21',
    scope: 'compile',
    'scorecard.score': 5.1,
    'provenance.present': false,
    label: 'Mature utility library, no provenance',
  },
  {
    name: 'event-stream',
    version: '4.0.1',
    type: 'library',
    group: '',
    purl: 'pkg:npm/event-stream@4.0.1',
    scope: 'compile',
    'scorecard.score': 1.5,
    'provenance.present': false,
    label: 'Known compromised package',
  },
  {
    name: 'colors',
    version: '1.4.0',
    type: 'library',
    group: '',
    purl: 'pkg:npm/colors@1.4.0',
    scope: 'compile',
    'scorecard.score': 2.0,
    'provenance.present': false,
    label: 'Known sabotaged package',
  },
  {
    name: 'helmet',
    version: '7.1.0',
    type: 'library',
    group: '',
    purl: 'pkg:npm/helmet@7.1.0',
    scope: 'compile',
    'scorecard.score': 8.5,
    'provenance.present': true,
    label: 'Excellent security posture',
  },
  {
    name: 'unknown-pkg',
    version: '0.1.0',
    type: 'library',
    group: '',
    purl: '',
    scope: 'compile',
    'scorecard.score': 0,
    'provenance.present': false,
    label: 'Unknown package, no data',
  },
]
