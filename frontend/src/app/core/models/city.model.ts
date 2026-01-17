export interface City {
  id: number;
  name: string;
  slug: string;
  active: boolean;
}

export interface CreateCityRequest {
  name: string;
  slug: string;
}
