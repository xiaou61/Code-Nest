import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  Empty,
  EmptyDescription,
  EmptyHeader,
  EmptyTitle,
  PageHeader,
} from '@paideia/ui'
import { Link } from 'react-router'

/**
 * 管理端首页：管理功能的入口列表。
 *
 * <p>仍然保留一段诚实的说明——目前只有知识库一项是真的，其余入口会随对应需求加入，
 * 不提前摆假入口。
 */
export function AdminHomePage() {
  return (
    <>
      <PageHeader title="管理端" description="已确认管理员身份。" />

      <Card>
        <CardHeader>
          <CardTitle>管理区</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <ul className="space-y-2">
            <li>
              <Link
                to="/knowledge"
                className="text-sm underline-offset-4 hover:underline"
                data-testid="admin-knowledge-link"
              >
                知识库：分类、条目与关系的录入
              </Link>
            </li>
          </ul>

          <Empty>
            <EmptyHeader>
              <EmptyTitle>其余管理功能还没有</EmptyTitle>
              <EmptyDescription>
                用户、课程与内容管理的入口会随对应需求加入，这里不提前摆假入口。
              </EmptyDescription>
            </EmptyHeader>
          </Empty>
        </CardContent>
      </Card>
    </>
  )
}
