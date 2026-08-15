export interface PetRenderer {
  mount(container: HTMLElement): Promise<void>
  loadModel(modelUrl: string): Promise<void>
  setExpression(expression: string): void
  playMotion(motion: string): void
  resize(width: number, height: number): void
  setVisible(visible: boolean): void
  dispose(): void
}

