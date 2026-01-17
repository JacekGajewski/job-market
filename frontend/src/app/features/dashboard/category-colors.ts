export interface CategoryColorConfig {
  slug: string;
  primary: string;
  gradient: string;
  glow: string;
}

export const CATEGORY_COLORS: Record<string, CategoryColorConfig> = {
  java: {
    slug: 'java',
    primary: '#00d4ff',
    gradient: 'rgba(0, 212, 255, 0.2)',
    glow: 'rgba(0, 212, 255, 0.4)'
  },
  python: {
    slug: 'python',
    primary: '#ffd700',
    gradient: 'rgba(255, 215, 0, 0.2)',
    glow: 'rgba(255, 215, 0, 0.4)'
  },
  devops: {
    slug: 'devops',
    primary: '#ff6b35',
    gradient: 'rgba(255, 107, 53, 0.2)',
    glow: 'rgba(255, 107, 53, 0.4)'
  },
  data: {
    slug: 'data',
    primary: '#a855f7',
    gradient: 'rgba(168, 85, 247, 0.2)',
    glow: 'rgba(168, 85, 247, 0.4)'
  },
  ai: {
    slug: 'ai',
    primary: '#00ff88',
    gradient: 'rgba(0, 255, 136, 0.2)',
    glow: 'rgba(0, 255, 136, 0.4)'
  },
  testing: {
    slug: 'testing',
    primary: '#ff3366',
    gradient: 'rgba(255, 51, 102, 0.2)',
    glow: 'rgba(255, 51, 102, 0.4)'
  }
};

export function getCategoryColor(slug: string): CategoryColorConfig {
  return CATEGORY_COLORS[slug] || {
    slug,
    primary: '#9e9e9e',
    gradient: 'rgba(158, 158, 158, 0.2)',
    glow: 'rgba(158, 158, 158, 0.4)'
  };
}
