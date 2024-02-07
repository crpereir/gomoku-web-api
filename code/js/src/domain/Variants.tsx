
export type Variants = {
  variants: Array<Variant>;
};

export type Variant = {
  variantId: number;
  boardDim: number;
  openingRules: string;
  playingRules: string;
};
