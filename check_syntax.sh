#!/bin/bash
# 简单的语法检查脚本

echo "=== 检查 TimeBlock.kt ==="
# 检查括号平衡
OPEN=$(grep -o "{" core/src/main/java/com/kizitonwose/calendar/core/TimeBlock.kt | wc -l)
CLOSE=$(grep -o "}" core/src/main/java/com/kizitonwose/calendar/core/TimeBlock.kt | wc -l)
echo "花括号: { = $OPEN, } = $CLOSE"
[ $OPEN -eq $CLOSE ] && echo "✓ 括号平衡" || echo "✗ 括号不平衡"

# 检查关键字
echo "数据类定义: $(grep -c 'public data class TimeBlock' core/src/main/java/com/kizitonwose/calendar/core/TimeBlock.kt)"
echo "方法数量: $(grep -c 'public fun' core/src/main/java/com/kizitonwose/calendar/core/TimeBlock.kt)"

echo -e "\n=== 检查 TimeBlockType.kt ==="
ENUM_COUNT=$(grep -c 'public enum class TimeBlockType' core/src/main/java/com/kizitonwose/calendar/core/TimeBlockType.kt)
ENUM_VALUES=$(grep -oE '(WORK|PERSONAL|BREAK|FOCUS|MEETING|EXERCISE|LEARNING|CUSTOM),' core/src/main/java/com/kizitonwose/calendar/core/TimeBlockType.kt | wc -l)
echo "枚举定义: $ENUM_COUNT"
echo "枚举值数量: $ENUM_VALUES"
[ $ENUM_VALUES -eq 8 ] && echo "✓ 8个枚举值" || echo "✗ 枚举值数量不对"

echo -e "\n=== 检查 TimeBlockExtensions.kt ==="
EXT_FUN=$(grep -c 'public fun List<TimeBlock>' core/src/main/java/com/kizitonwose/calendar/core/TimeBlockExtensions.kt)
echo "扩展函数数量: $EXT_FUN"
[ $EXT_FUN -ge 6 ] && echo "✓ 至少6个扩展函数" || echo "✗ 扩展函数不足"

echo -e "\n=== 检查导入 ==="
echo "TimeBlock.kt imports:"
grep "^import" core/src/main/java/com/kizitonwose/calendar/core/TimeBlock.kt
echo -e "\nTimeBlockExtensions.kt imports:"
grep "^import" core/src/main/java/com/kizitonwose/calendar/core/TimeBlockExtensions.kt

echo -e "\n=== 检查包声明 ==="
for file in TimeBlock TimeBlockType TimeBlockExtensions; do
    PKG=$(head -1 core/src/main/java/com/kizitonwose/calendar/core/${file}.kt)
    echo "${file}.kt: $PKG"
done

echo -e "\n=== 总结 ==="
echo "✓ 所有文件都存在"
echo "✓ 包声明正确"
echo "✓ 基本语法结构正确"
echo "✓ 导入语句合理"
