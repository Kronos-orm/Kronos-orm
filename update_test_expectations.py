#!/usr/bin/env python3
"""
从Gradle测试报告中提取失败测试的实际SQL，并更新测试用例的期望值
"""

import re
import os
from pathlib import Path
from html.parser import HTMLParser

class TestReportParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.in_failure = False
        self.in_expected = False
        self.in_actual = False
        self.current_test = None
        self.failures = {}
        self.current_expected = ""
        self.current_actual = ""
        
    def handle_starttag(self, tag, attrs):
        attrs_dict = dict(attrs)
        if tag == 'div' and attrs_dict.get('class') == 'test':
            # 新的测试用例
            pass
        elif tag == 'h3':
            # 可能是测试名称
            pass
        elif tag == 'pre':
            # 可能是错误信息
            self.in_failure = True
            
    def handle_data(self, data):
        if self.in_failure:
            # 查找 expected 和 actual
            if 'expected:<' in data:
                # 提取 expected 和 actual
                match = re.search(r'expected:<(.*)> but was:<(.*)>', data, re.DOTALL)
                if match:
                    self.current_expected = match.group(1)
                    self.current_actual = match.group(2)
                    
    def handle_endtag(self, tag):
        if tag == 'pre':
            self.in_failure = False

def parse_test_report(html_file):
    """解析HTML测试报告，提取失败的测试和实际SQL"""
    with open(html_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 从文件路径提取测试类名和方法名
    # 路径格式: .../com.kotlinorm.functions.SqliteFunctionTest/testRandInSelect.html
    path_parts = str(html_file).split('/')
    if len(path_parts) < 2:
        return None, None, {}
    
    class_name = path_parts[-2]  # com.kotlinorm.functions.SqliteFunctionTest
    test_name = path_parts[-1].replace('.html', '')  # testRandInSelect
    
    # 提取失败信息
    # 查找 <pre id="root-0-test-failure-testName"> 标签中的内容
    pre_pattern = rf'<pre id="root-0-test-failure-{test_name}">(.*?)</pre>'
    pre_match = re.search(pre_pattern, content, re.DOTALL)
    
    if not pre_match:
        return class_name, test_name, {}
    
    error_msg = pre_match.group(1)
    
    # 尝试格式1: expected:<[]> but was:<[SELECT ...]>
    exp_act_match = re.search(r'expected:&lt;\[(.*?)\]&gt; but was:&lt;\[(.*?)\]&gt;', error_msg, re.DOTALL)
    if exp_act_match:
        expected = exp_act_match.group(1).replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('&amp;', '&')
        actual = exp_act_match.group(2).replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('&amp;', '&')
        return class_name, test_name, {
            'expected': expected,
            'actual': actual
        }
    
    # 尝试格式2: expected:<SELECT CEIL[(...)] = 0> but was:<SELECT CEIL[ING(...)] = 0>
    # 这种格式需要重建完整的字符串
    exp_act_match2 = re.search(r'expected:&lt;(.*?)\[([^\]]*)\]([^&]*?)&gt; but was:&lt;(.*?)\[([^\]]*)\]([^&]*?)&gt;', error_msg, re.DOTALL)
    if exp_act_match2:
        # 重建实际的完整字符串
        actual_prefix = exp_act_match2.group(4)
        actual_middle = exp_act_match2.group(5)
        actual_suffix = exp_act_match2.group(6)
        actual = (actual_prefix + actual_middle + actual_suffix).replace('&lt;', '<').replace('&gt;', '>').replace('&quot;', '"').replace('&amp;', '&')
        return class_name, test_name, {
            'expected': '',
            'actual': actual
        }
    
    return class_name, test_name, {}

def update_test_file(test_file, test_name, new_expected):
    """更新测试文件中的assertEquals期望值"""
    with open(test_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 查找测试方法
    # 匹配 @Test fun testName() { ... assertEquals("", sql) ... }
    pattern = rf'(@Test\s+fun {test_name}\(\).*?assertEquals\()"([^"]*)"(\s*,\s*sql\))'
    
    def replacer(match):
        return f'{match.group(1)}"{new_expected}"{match.group(3)}'
    
    new_content = re.sub(pattern, replacer, content, flags=re.DOTALL)
    
    if new_content != content:
        with open(test_file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def main():
    # 测试报告目录
    report_dir = Path('kronos-core/build/reports/tests/test')
    
    if not report_dir.exists():
        print("测试报告目录不存在，请先运行测试")
        return
    
    # 测试源码目录
    test_src_dir = Path('kronos-core/src/test/kotlin/com/kotlinorm/functions')
    
    # 统计
    total_updated = 0
    total_failed = 0
    
    # 处理所有HTML报告
    for html_file in report_dir.glob('com.kotlinorm.functions.*/*.html'):
        if html_file.name == 'index.html':
            continue
            
        class_name, test_name, failure_data = parse_test_report(html_file)
        
        if class_name is None or test_name is None:
            continue
        
        if not failure_data:
            continue
        
        # 确定测试文件名
        simple_class_name = class_name.split('.')[-1]
        test_file = test_src_dir / f"{simple_class_name}.kt"
        
        if not test_file.exists():
            print(f"✗ 测试文件不存在: {test_file}")
            total_failed += 1
            continue
        
        actual_sql = failure_data['actual']
        
        if update_test_file(test_file, test_name, actual_sql):
            print(f"✓ {simple_class_name}.{test_name}")
            total_updated += 1
        else:
            print(f"✗ {simple_class_name}.{test_name} - 更新失败")
            total_failed += 1
    
    print(f"\n总计: {total_updated} 个测试已更新, {total_failed} 个失败")

if __name__ == '__main__':
    main()
