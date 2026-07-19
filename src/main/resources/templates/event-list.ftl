<#include "_header.ftl">
<section>
  <div class="page-head">
    <h1>イベント一覧</h1>
    <a class="button" href="/g/${guildId?c}/events/new">＋ イベント作成</a>
  </div>
  <div class="tabs">
    <a class="tab <#if status == 'recruiting'>active</#if>" href="/g/${guildId?c}/events?status=recruiting">募集中</a>
    <a class="tab <#if status == 'archived'>active</#if>" href="/g/${guildId?c}/events?status=archived">アーカイブ</a>
  </div>

  <#if events?size == 0>
    <p class="empty">イベントがありません。</p>
  <#else>
    <table class="event-table">
      <thead>
        <tr>
          <th>第n回</th>
          <th>名前</th>
          <th>エリア</th>
          <th>開催日</th>
          <th>幹事</th>
          <th>参加人数</th>
        </tr>
      </thead>
      <tbody>
        <#list events as event>
          <tr>
            <td>第${event.number}回</td>
            <td><a href="/g/${guildId?c}/events/${event.id?c}">${event.name}</a></td>
            <td>${event.area}</td>
            <td>${event.date}</td>
            <td>${event.organizerName}</td>
            <td>${event.participantCount!""}</td>
          </tr>
        </#list>
      </tbody>
    </table>
  </#if>
</section>
<#include "_footer.ftl">
