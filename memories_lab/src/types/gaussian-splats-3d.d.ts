declare module '@mkkellogg/gaussian-splats-3d' {
  import * as THREE from 'three';

  export interface ViewerOptions {
    cameraUp?: number[];
    initialCameraPosition?: number[];
    initialCameraLookAt?: number[];
    sphericalHarmonicsDegree?: number;
    renderer?: THREE.WebGLRenderer;
    camera?: THREE.Camera;
    scene?: THREE.Scene;
  }

  export interface AddSplatSceneOptions {
    progressiveLoad?: boolean;
    onProgress?: (percent: number) => void;
  }

  export class Viewer {
    constructor(options?: ViewerOptions);
    addSplatScene(url: string, options?: AddSplatSceneOptions): Promise<void>;
    start(): void;
    stop(): void;
    dispose(): void;
  }
}
