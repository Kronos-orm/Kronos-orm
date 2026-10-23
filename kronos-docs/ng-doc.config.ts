import { NgDocConfiguration } from '@ng-doc/builder';

const config: NgDocConfiguration = {
    // Static export reads every generated page wrapper after each build.
    cache: false,
    repoConfig: {
        url: 'https://github.com/Kronos-orm/Kronos-orm',
        mainBranch: 'main',
        releaseBranch: 'main',
    },
    routePrefix: 'documentation',
};

export default config;
