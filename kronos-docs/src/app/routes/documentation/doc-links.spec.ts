import {documentationAnchorUrl, repositorySourceUrl} from './doc-links';

describe('documentationAnchorUrl', () => {
    it('keeps the router path and heading fragment in hash mode', () => {
        expect(documentationAnchorUrl(
            'https://kotlinorm.com',
            '/',
            '/documentation/zh-CN/configuration/global-config',
            '日志输出路径及开关'
        )).toBe(
            'https://kotlinorm.com/#/documentation/zh-CN/configuration/global-config#%E6%97%A5%E5%BF%97%E8%BE%93%E5%87%BA%E8%B7%AF%E5%BE%84%E5%8F%8A%E5%BC%80%E5%85%B3'
        );
    });
});

describe('repositorySourceUrl', () => {
    it('prefixes the nested documentation application directory', () => {
        expect(repositorySourceUrl(
            'https://github.com/Kronos-orm/Kronos-orm/edit/main/src/app/docs/zh-CN/6.configuration/logging/index.md'
        )).toBe(
            'https://github.com/Kronos-orm/Kronos-orm/edit/main/kronos-docs/src/app/docs/zh-CN/6.configuration/logging/index.md'
        );
    });

    it('does not duplicate an existing repository directory', () => {
        const url = 'https://github.com/Kronos-orm/Kronos-orm/blob/main/kronos-docs/src/app/docs/en/index.md';
        expect(repositorySourceUrl(url)).toBe(url);
    });
});
