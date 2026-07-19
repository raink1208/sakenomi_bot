<#if addableMembers?size == 0>
<option value="" disabled selected>追加可能なメンバーがいません</option>
<#else>
<#list addableMembers as m>
<option value="${m.id?c}">${m.name}</option>
</#list>
</#if>
